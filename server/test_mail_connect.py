import base64
import copy
import json
import time
import unittest
from unittest.mock import MagicMock, patch
from urllib.parse import parse_qs, urlsplit
from flask import Flask, request
import mail_connect as mail
import main
import document_analysis
from server import build_request
from test_document_analysis import sample

class MailTests(unittest.TestCase):
    def test_gmail_and_microsoft_read_only_scopes(self):
        for provider in mail.PROVIDERS.values():
            scope=provider['scope'].lower()
            self.assertNotIn('write',scope)
            self.assertNotIn('send',scope)
            self.assertNotIn('modify',scope)

    def test_unconfigured_provider_never_opens_fake_consent(self):
        db=MagicMock();db.collection.return_value.document.return_value.get.return_value.to_dict.return_value={}
        with self.assertRaises(mail.MailError):mail.start(db,'owner','google')

    def test_oauth_state_pkce_and_fixed_redirect(self):
        db=MagicMock()
        with patch.object(mail,'config',return_value={'clientId':'public-id','clientSecret':'secret'}):
            result=mail.start(db,'owner','google')
        query=parse_qs(urlsplit(result['url']).query)
        self.assertEqual(query['code_challenge_method'],['S256'])
        self.assertEqual(query['redirect_uri'],[mail.BASE+'/mail/callback/google'])
        self.assertNotIn('secret',result['url'])
        self.assertEqual(query['access_type'],['offline'])
        stored=db.collection.return_value.document.return_value.set.call_args.args[0]
        self.assertEqual(stored['stateHash'],mail.digest(query['state'][0]))
        self.assertNotIn('state',stored)
        self.assertLess(stored['expires'],time.time()+601)

    def test_account_lookup_is_owner_scoped(self):
        db=MagicMock();mail.account_ref(db,'owner-a','a'*64)
        db.collection.assert_called_once_with('_mailUsers')
        db.collection.return_value.document.assert_called_once_with('owner-a')
        with self.assertRaises(mail.MailError):mail.account_ref(db,'owner-a','../owner-b')

    def test_callback_rejects_missing_or_path_in_state(self):
        for state in ('','owner','../victim.token','a/b.token'):
            with self.assertRaises(mail.MailError):mail.callback(MagicMock(),'google',{'state':state,'code':'test'})

    def test_callback_rejects_expired_replayed_or_wrong_provider(self):
        db=MagicMock();ref=db.collection.return_value.document.return_value
        good={'stateHash':mail.digest('owner.random'),'expires':time.time()+600,'provider':'google','verifier':'x'}
        for change in ({'expires':0},{'stateHash':'different'},{'provider':'microsoft'},{}):
            value={**good,**change} if change else {}
            ref.get.return_value.to_dict.return_value=value
            with patch.object(mail.firestore,'transactional',side_effect=lambda f:f),patch.object(mail,'remote') as remote:
                with self.assertRaises(mail.MailError):mail.callback(db,'google',{'state':'owner.random','code':'code'})
                remote.assert_not_called()

    def test_graph_pagination_cannot_exfiltrate_token(self):
        for url in ('http://graph.microsoft.com/v1.0/me/messages','https://evil.test/v1.0/me/messages',
                    'https://graph.microsoft.com@evil.test/v1.0/me/messages','https://graph.microsoft.com/v1.0/users/other/messages',
                    'https://graph.microsoft.com/v1.0/me/messages#anything'):
            with self.assertRaises(mail.MailError):mail.graph_cursor(url)
        valid='https://graph.microsoft.com/v1.0/me/messages?$skip=5'
        self.assertEqual(mail.graph_cursor(valid),valid)

    def test_oauth_callback_cannot_recreate_account_during_erasure(self):
        db=MagicMock();pending=MagicMock();marker=MagicMock()
        pending.get.return_value.to_dict.return_value={'stateHash':mail.digest('owner.random'),'expires':time.time()+600,'provider':'google','verifier':'x'}
        marker.get.return_value.exists=True
        db.collection.side_effect=lambda name: MagicMock(document=MagicMock(return_value=marker if name=='_accountDeletion' else pending))
        with patch.object(mail.firestore,'transactional',side_effect=lambda f:f),patch.object(mail,'config',return_value={'clientId':'id','clientSecret':'secret'}),patch.object(mail,'remote',side_effect=[{'scope':'https://www.googleapis.com/auth/gmail.readonly','refresh_token':'refresh','access_token':'access'},{'emailAddress':'fixture@example.invalid'}]):
            with self.assertRaises(mail.MailError) as error:mail.callback(db,'google',{'state':'owner.random','code':'code'})
            self.assertEqual(error.exception.status,410)
            db.transaction.return_value.set.assert_not_called()

    def test_nested_gmail_attachment_and_inline_logo(self):
        attachment={'filename':'resultado.pdf','mimeType':'application/pdf','body':{'attachmentId':'a','size':100}}
        logo={'filename':'medical-logo.png','mimeType':'image/png','body':{'attachmentId':'b','size':100},'headers':[{'name':'Content-Disposition','value':'inline'}]}
        result=mail.gmail_parts('m',{'parts':[{'parts':[attachment]},logo]},'Resultados de laboratorio')
        self.assertEqual([x['attachment'] for x in result],['a'])

    def test_filter_formats_size_and_unrelated_mail(self):
        self.assertIsNone(mail.file_meta('m','a','scan.pdf','application/pdf',100,'Historia clínica'))
        self.assertIsNone(mail.file_meta('m','a','factura.pdf','application/pdf',100,'Compra de zapatos'))
        self.assertIsNone(mail.file_meta('m','a','resultado.exe','application/octet-stream',100,'Examen'))
        self.assertIsNone(mail.file_meta('m','a','resultado.pdf','application/pdf',mail.MAX_FILE+1,'Examen'))

    def test_history_filename_separators_and_clinical_information_subject(self):
        self.assertIsNotNone(mail.file_meta('m','a','historia_clinica_87845.pdf','application/pdf',100,'Se ha compartido información clínica'))
        for name in ('historia_clinica_87845.pdf','HISTORIA-CLÍNICA-87845.pdf','historia.clinica.87845.pdf'):
            self.assertTrue(mail.clinical_hint('',name))
        self.assertFalse(mail.clinical_hint('Se ha compartido información clínica','archivo.pdf'))
        self.assertFalse(mail.clinical_hint('Se ha compartido información','factura_87845.pdf'))

    def test_scan_returns_cursor_without_mutating_checkpoint(self):
        db=MagicMock();now=int(time.time())
        with patch.object(mail,'access',return_value=('google','token')),patch.object(mail,'remote',return_value={'messages':[],'nextPageToken':'page2'}):
            result=mail.scan(db,'owner',{'account':'a'*64,'since':now-100,'until':now})
        self.assertEqual(result,{'attachments':[],'cursor':'page2'})
        db.assert_not_called()

    def test_patient_name_and_history_without_clinical_word_reaches_analysis(self):
        for name in ('ESTEBAN BERRIO HISTORIA.pdf','ESTEBAN_BERRIO_HISTORIA.pdf',
                     'Historia.pdf','HISTORIAS.pdf','Historial.pdf'):
            payload={'parts':[{'filename':name,'mimeType':'application/pdf',
                              'body':{'attachmentId':'attachment','size':2048}}]}
            result=mail.gmail_parts('message',payload,'Documentos compartidos')
            self.assertEqual([item['name'] for item in result],[name])
        self.assertFalse(mail.clinical_hint('Compra','historiador_factura.pdf'))
        self.assertIsNone(mail.file_meta('m','a','HISTORIA.pdf','application/pdf',mail.MAX_FILE+1,''))
        self.assertIsNone(mail.file_meta('m','a','HISTORIA.png','image/png',100,'',inline=True))

    def test_invalid_scan_range(self):
        with patch.object(mail,'access',return_value=('google','token')):
            with self.assertRaises(mail.MailError):mail.scan(MagicMock(),'u',{'account':'a'*64,'since':0,'until':int(time.time())})

    def test_self_sent_inbox_history_is_a_candidate(self):
        now=int(time.time())
        message={'labelIds':['SENT','INBOX'],'payload':{'headers':[{'name':'Subject','value':'Historia clínica'}],
            'parts':[{'filename':'historia.pdf','mimeType':'application/pdf','body':{'attachmentId':'a','size':100}}]}}
        def remote(url, token):
            if '/messages?' in url:
                query=parse_qs(urlsplit(url).query)['q'][0]
                self.assertIn('{-in:sent in:inbox}',query)
                self.assertIn('-in:drafts',query)
                self.assertIn('-in:spam',query)
                self.assertIn('-in:trash',query)
                return {'messages':[{'id':'m'}]}
            return message
        with patch.object(mail,'access',return_value=('google','token')),patch.object(mail,'remote',side_effect=remote):
            result=mail.scan(MagicMock(),'owner',{'account':'a'*64,'since':now-100,'until':now})
        self.assertEqual([item['name'] for item in result['attachments']],['historia.pdf'])

    def test_attachment_keywords_unicode_and_extra_words_ignore_subject(self):
        names=['ESTEBAN BERRIO HISTORIA.pdf','paciente_historía_87845.pdf',
               'AYUDAS_DIAGNÓSTICAS (2026) paciente.pdf','Paciente ayudas--diagnosticas.pdf',
               'Juan RESULTADOS #123.pdf','resultado(1).pdf','mis_exámenes_2026.pdf',
               '123HISTORIA87845.pdf','PacienteHistoriaClinica.pdf','histo\u200bria.pdf',
               'ＨＩＳＴＯＲＩＡ.pdf','historí́a.pdf','🩺—exámenes—Juan.pdf']
        for name in names:
            with self.subTest(name=name):
                self.assertIsNotNone(mail.file_meta('m','a',name,'application/pdf',100,'Sin asunto'))
        payload={'parts':[{'filename':n,'mimeType':'application/pdf','body':{'attachmentId':str(i),'size':100}}
                          for i,n in enumerate(['HISTORÍA.pdf','factura.pdf','foto.pdf','resultados.pdf'])]}
        for subject in ('','HISTORIA CLINICA RESULTADOS EXAMENES','Compra de zapatos'):
            self.assertEqual([x['name'] for x in mail.gmail_parts('m',payload,subject)],['HISTORÍA.pdf','resultados.pdf'])

    def test_download_checks_magic_not_filename(self):
        with patch.object(mail,'access',return_value=('google','token')),patch.object(mail,'remote',return_value={'data':base64.urlsafe_b64encode(b'<html>not a PDF</html>').decode()}):
            with self.assertRaises(mail.MailError):mail.download(MagicMock(),'u',{'account':'a'*64,'message':'m','attachment':'a'})

    def test_disconnect_does_not_restore_credentials_on_revoke_failure(self):
        db=MagicMock();ref=db.collection.return_value.document.return_value.collection.return_value.document.return_value
        ref.get.return_value.to_dict.return_value={'provider':'google','refresh':'secret'}
        with patch.object(mail,'remote',side_effect=mail.MailError('offline')):
            result=mail.disconnect(db,'owner','a'*64)
        ref.delete.assert_called_once();self.assertTrue(result['disconnected'])

    def test_status_never_contains_refresh_token(self):
        snapshot=MagicMock();snapshot.id='a'*64;snapshot.to_dict.return_value={'provider':'google','email':'fixture@example.invalid','refresh':'private','generation':'hidden'}
        self.assertEqual(set(mail.public_account(snapshot)),{'id','provider','email'})

    def test_automatic_document_requires_patient_and_classification(self):
        data={'mailImport':True,'patient':{'fullName':'Persona de prueba','birthDate':'1995-01-01'},'document':{'mime':'application/pdf','data':base64.b64encode(b'%PDF-1.4 fixture').decode()}}
        body=document_analysis.request(data,build_request)
        self.assertIn('patient_match',body['text']['format']['schema']['required'])
        self.assertIn('document_kind',body['text']['format']['schema']['required'])
        self.assertNotIn('tools',body)
        data['patient']={}
        with self.assertRaises(ValueError):document_analysis.request(data,build_request)

    def test_invalid_automatic_classification_rejected(self):
        value={**sample(),'document_kind':'history','patient_match':'unknown'}
        self.assertEqual(document_analysis.validate(value)['patient_match'],'unknown')
        value['patient_match']='probably'
        with self.assertRaises(ValueError):document_analysis.validate(value)

    def invoke(self,path,identity):
        with patch.object(main.auth,'verify_id_token',return_value=identity),patch.object(main.firestore,'client'),patch.object(main.mail_connect,'dispatch') as dispatch:
            with Flask(__name__).test_request_context(path,method='POST',headers={'Authorization':'Bearer fake'},json={}):
                response=main.bio_api(request)
            return response,dispatch

    def test_anonymous_cannot_connect_mail(self):
        response,dispatch=self.invoke('/mail/connect',{'uid':'test','firebase':{'sign_in_provider':'anonymous'}})
        self.assertEqual(response.status_code,403);dispatch.assert_not_called()

    def test_mail_route_uses_verified_owner_not_request_owner(self):
        with patch.object(main.auth,'verify_id_token',return_value={'uid':'owner'}),patch.object(main.firestore,'client'),patch.object(main.mail_connect,'dispatch',return_value={'accounts':[]}) as dispatch:
            with Flask(__name__).test_request_context('/mail/status',method='POST',headers={'Authorization':'Bearer fake'},json={'uid':'victim'}):
                response=main.bio_api(request)
            self.assertEqual(response.status_code,200)
            self.assertEqual(dispatch.call_args.args[1],'owner')

if __name__=='__main__':unittest.main()
