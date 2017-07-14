import sys
if sys.version_info < (2, 7):
    import unittest2 as unittest
else:
    import unittest
import ast
import json
import os
import os.path
import string
import subprocess
import requests

sys.path.append("../../cmd")

TEST_RESOURCES_PATH = '../tests/resources/'    # Trailing '/' is required
CRED_FILENAME = 'hrls_credentials'
CRED_PATH = TEST_RESOURCES_PATH+CRED_FILENAME

if 'CRED_PATH' in os.environ:
    CRED_PATH = os.environ['CRED_PATH']


@unittest.skipUnless(os.path.isfile(CRED_PATH) and os.access(CRED_PATH, os.R_OK),
                     'requires HRLS credentials file at %s' % CRED_PATH)

def execute_curl(url, username, password, search_list=None, search_verify=None ):
    '''run curl command, get output back in an list'''

    auth = (username, password)

    if search_list is not None:
        url=url+"?"+'%'.join(search_list)

    if search_verify == 'True' or search_verify is None:
        r = requests.get(url, auth=auth)
    elif search_verify == 'False':
        r = requests.get(url, auth=auth, verify=False)
    else:
        r = requests.get(url, auth=auth, verify=search_verify)
    return r


class HrlsIntegrationTests(unittest.TestCase):

    def setUp(self):
        """Setup testB2SafeCmd environment before the tests have run"""
        jsonfilecontent = json.loads(open(CRED_PATH, 'r').read())
        self.handle_server_url = jsonfilecontent.pop('handle_server_url')
        self.prefix = jsonfilecontent.pop('prefix')
        self.username = jsonfilecontent.pop('reverselookup_username')
        self.password = jsonfilecontent.pop('reverselookup_password')
        self.https_verify = jsonfilecontent.pop('HTTPS_verify')
 

    def tearDown(self):
        """ Cleanup testB2SafeCmd environment after the tests have run
        """
        no_op=None

    def test_ping(self):
        """Test that ping works."""
        ping_result = execute_curl(self.handle_server_url+'/hrls/ping', self.username, self.password, None, self.https_verify)
        self.assertEqual(
            ping_result.status_code, 200,
            'ping hrls returns unexpected status')
        self.assertEqual(
            ping_result.content, 'OK\n',
            'ping hrls returns unexpected response')

    def test_search_handle_by_non_existing_key_value(self):
        """Test that search by ['URL=my_unknown_handle_url'] returns no matching handle."""
        search_array=['URL=my_unknown_handle_url']
        search_result = execute_curl(self.handle_server_url+'/hrls/handles', self.username, self.password, search_array, self.https_verify)
        self.assertEqual(
            search_result.status_code, 200,
            'search hrls by non existing url key returns unexpected status')
        self.assertEqual(
            search_result.content, '[]',
            'search handle by non existing url key returns unexpected response')

    def test_search_handle_by_non_existing_and_existing_key_value(self):
        """Test that search by ['URL=my_unknown_handle_url','HS_ADMIN=*'] returns no matching handle."""
        search_array=['URL=my_unknown_handle_url','HS_ADMIN=*']
        search_result = execute_curl(self.handle_server_url+'/hrls/handles', self.username, self.password, search_array, self.https_verify)
        self.assertEqual(
            search_result.status_code, 200,
            'search hrls by non existing url key returns unexpected status')
        self.assertEqual(
            search_result.content, '[]',
            'search handle by non existing url key returns unexpected response')

    def test_search_handle_by_existing_and_non_existing_key_value(self):
        """Test that search by ['HS_ADMIN=*','URL=my_unknown_handle_url'] returns no matching handle."""
        search_array=['HS_ADMIN=*','URL=my_unknown_handle_url']
        search_result = execute_curl(self.handle_server_url+'/hrls/handles', self.username, self.password, search_array, self.https_verify)
        self.assertEqual(
            search_result.status_code, 200,
            'search hrls by non existing url key returns unexpected status')
        self.assertEqual(
            search_result.content, '[]',
            'search handle by non existing url key returns unexpected response')


