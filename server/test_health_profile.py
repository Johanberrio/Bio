import base64
import json
import unittest
from server import build_request
from document_analysis import request

class ProfileTests(unittest.TestCase):
    def profile(self):
        return dict(ageYears=30,heightCm=170,weightKg=65,bloodGroup="No lo sé",asOf="2026-09-28",fullName="PRIVATE NAME",birthDate="PRIVATE DATE",refresh_token="SECRET")
    def test_context_excludes_identity_and_secrets(self):
        r=build_request(dict(query="Revisar",profile=self.profile()))
        context=json.loads(r['input'][0]['content'][0]['text'])
        self.assertEqual(context['perfil_declarado']['weightKg'],65)
        self.assertNotIn('PRIVATE',str(r));self.assertNotIn('SECRET',str(r))
    def test_profile_reaches_document_analysis_without_web(self):
        r=request(dict(profile=self.profile(),document=dict(mime="application/pdf",data=base64.b64encode(b'%PDF-1.7 sample').decode())),build_request)
        self.assertIn('heightCm',r['input'][0]['content'][0]['text'])
        self.assertNotIn('tools',r)
        self.assertIn('nunca es evidencia',r['instructions'])
    def test_invalid_profile_rejected(self):
        for key,value in [('ageYears',True),('heightCm',float('nan')),('weightKg',-1),('bloodGroup','inventado'),('asOf','2999-01-01')]:
            p=self.profile();p[key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):build_request(dict(query='x',profile=p))
    def test_vitals_keep_provenance(self):
        r=build_request(dict(query='x',vitals=[dict(metric=0,value=70,created=1,origin='Health Connect',sourceRecord='PRIVATE-ID')]))
        text=r['input'][0]['content'][0]['text']
        self.assertIn('Health Connect',text);self.assertNotIn('mediciones_manuales',text);self.assertNotIn('PRIVATE-ID',text)
