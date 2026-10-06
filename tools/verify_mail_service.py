"""Opt-in live Bio test. Creates and deletes synthetic Firebase users; reads no mailbox."""
import json
import pathlib
import secrets
import urllib.request
import urllib.error
import urllib.parse
import uuid

ROOT=pathlib.Path(__file__).resolve().parents[1]
config=json.loads((ROOT/'app/src/main/assets/firebase-config.json').read_text())
assert config['projectId']=='bio-project-15d15'
BASE=json.loads((ROOT/'app/src/main/assets/backend-config.json').read_text())['url']

def post(url,data,token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(url,data=json.dumps(data).encode(),headers=headers)
    try:
        with urllib.request.urlopen(req,timeout=90) as response:return response.status,json.load(response)
    except urllib.error.HTTPError as error:
        return error.code,{}

def auth(action,data):
    code,result=post('https://identitytoolkit.googleapis.com/v1/accounts:'+action+'?key='+config['apiKey'],data)
    if code!=200:raise RuntimeError('Synthetic account operation failed: '+action+' '+str(code))
    return result

assert post(BASE+'/mail/status',{})[0]==401
for anonymous in (True,False):
    data={'returnSecureToken':True}
    if not anonymous:data.update(email='bio-mail-fixture-'+uuid.uuid4().hex+'@example.invalid',password=secrets.token_urlsafe(32))
    token=auth('signUp',data)['idToken']
    try:
        code,result=post(BASE+'/mail/status',{},token)
        assert code==(403 if anonymous else 200), 'Unexpected mail status '+str(code)
        if not anonymous:
            assert result['accounts']==[]
            assert set(result['providers'])=={'google','microsoft'}
            print('Live provider availability:',result['providers'])
            for provider,enabled in result['providers'].items():
                if not enabled:
                    assert post(BASE+'/mail/connect',{'provider':provider},token)[0]==503
                else:
                    code,flow=post(BASE+'/mail/connect',{'provider':provider},token)
                    assert code==200
                    parsed=urllib.parse.urlsplit(flow['url']);query=urllib.parse.parse_qs(parsed.query)
                    expected='accounts.google.com' if provider=='google' else 'login.microsoftonline.com'
                    assert parsed.scheme=='https' and parsed.netloc==expected
                    assert query['code_challenge_method']==['S256']
                    callback='https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/mail/callback/'+provider
                    assert query['redirect_uri']==[callback]
                    # Simulated cancellation consumes the temporary state, without opening a mailbox.
                    with urllib.request.urlopen(callback+'?'+urllib.parse.urlencode({'state':query['state'][0],'error':'access_denied'}),timeout=90) as response:
                        assert 'Conexión cancelada' in response.read().decode('utf-8')
                    with urllib.request.urlopen(callback+'?'+urllib.parse.urlencode({'state':query['state'][0],'error':'access_denied'}),timeout=90) as response:
                        assert 'vencida' in response.read().decode('utf-8')
                    print('PASS: OAuth start, fixed callback, PKCE, cancellation and replay rejection for',provider)
            # A normal Bio account must never read the server's OAuth secret collection.
            private='https://firestore.googleapis.com/v1/projects/bio-project-15d15/databases/(default)/documents/_mailConfig/google'
            try:
                with urllib.request.urlopen(urllib.request.Request(private,headers={'Authorization':'Bearer '+token}),timeout=90) as response:
                    raise AssertionError('Private OAuth config unexpectedly readable by Firebase user')
            except urllib.error.HTTPError as error:
                assert error.code==403
            print('PASS: private OAuth configuration inaccessible to Firebase users')
            assert post(BASE+'/mail/download',{'account':'../another-owner'},token)[0]==400
    finally:
        auth('delete',{'idToken':token})
print('PASS: authenticated mail routes, isolated empty account, disabled-provider feedback; synthetic users deleted. No mailbox accessed.')
