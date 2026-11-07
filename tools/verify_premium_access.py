"""Live access checks only: never invokes a model or makes a Play purchase."""
import json
import secrets
from pathlib import Path
import requests

BASE = 'https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api'


def main():
    cfg = json.loads(Path('app/src/main/assets/firebase-config.json').read_text())
    if cfg['projectId'] != 'bio-project-15d15':
        raise RuntimeError('Unexpected project')
    auth_url = 'https://identitytoolkit.googleapis.com/v1/accounts:'
    user = None
    try:
        response = requests.get(BASE + '/health', timeout=30)
        assert response.status_code == 200 and response.json()['project'] == cfg['projectId']
        print('PASS health', flush=True)
        for path in ('/billing/status', '/analyze-document', '/research'):
            assert requests.post(BASE + path, json={}, timeout=30).status_code == 401
        print('PASS unauthenticated requests rejected', flush=True)
        response = requests.post(auth_url + 'signUp', params={'key': cfg['apiKey']}, json={
            'email': 'bio-access-' + secrets.token_hex(8) + '@example.invalid',
            'password': secrets.token_urlsafe(32), 'returnSecureToken': True}, timeout=30)
        if response.status_code != 200:
            raise RuntimeError('Temporary account creation failed')
        user = response.json()
        headers = {'Authorization': 'Bearer ' + user['idToken']}
        response = requests.post(BASE + '/billing/status', headers=headers, json={}, timeout=30)
        assert response.status_code == 200
        status = response.json()
        assert status['premium'] is False and status['purchasesEnabled'] is False
        print('PASS Normal account; purchases disabled', flush=True)
        usage = status['usage']
        assert usage['timezone'] == 'UTC'
        for kind, limit in (('analysis', 30), ('research', 20)):
            assert usage[kind] == {'limit': limit, 'used': 0, 'remaining': limit}
        print('PASS deployed monthly allowances: 30 analyses and 20 queries', flush=True)
        private_url = ('https://firestore.googleapis.com/v1/projects/' + cfg['projectId']
                       + '/databases/(default)/documents/_premiumUsage/' + user['localId']
                       + '/months/' + usage['period'])
        assert requests.get(private_url, headers=headers, timeout=30).status_code == 403
        assert requests.patch(private_url, headers=headers,
                              json={'fields': {'analysis': {'integerValue': '0'}}},
                              timeout=30).status_code == 403
        print('PASS clients cannot read or modify private quota counters', flush=True)
        # Empty payload cannot reach generation even if a regression bypasses billing.
        for path in ('/analyze-document', '/research'):
            response = requests.post(BASE + path, headers=headers, json={}, timeout=30)
            assert response.status_code == 402 and response.json()['code'] == 'premium_required'
        print('PASS both AI routes restricted to Premium', flush=True)
    finally:
        if user:
            response = requests.post(auth_url + 'delete', params={'key': cfg['apiKey']},
                                     json={'idToken': user['idToken']}, timeout=30)
            if response.status_code != 200:
                raise RuntimeError('Temporary account cleanup failed')
            print('PASS temporary account removed', flush=True)


if __name__ == '__main__':
    main()
