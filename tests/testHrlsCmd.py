#!/usr/bin/env python

from testHrlsCmd.hrlsintgtest import HrlsIntegrationTests

import argparse
import unittest

__author__ = 'RobertV'

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Test script for hrls ')
    parser.add_argument('-test', action='store', dest='script',
                        help='[hrls]')

    param = parser.parse_args()

    if param.script == "hrls":
        # test cases for B2HANDLE-Hrls#
        print "Test hrls Script"
        hrls_suite = unittest.TestLoader().loadTestsFromTestCase(HrlsIntegrationTests)
        unittest.TextTestRunner(descriptions=2, verbosity=2).run(hrls_suite)

    else:
        print "Invalid Input; Valid example ./testHrlsCmd -test hrls"
