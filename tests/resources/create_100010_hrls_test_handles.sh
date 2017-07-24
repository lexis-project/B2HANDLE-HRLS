#!/bin/bash

PREFIX=$1

echo "AUTHENTICATE PUBKEY:300:0.NA/${PREFIX}"
echo "/<path>/admpriv.bin"
echo ""

for i in `seq -w 1 100010`
do
    echo "CREATE ${PREFIX}/HRLS_CHECK_HANDLE_${i}"
    echo "1   URL       86400 1110 UTF8 http://www.test_hrls_check.com/${i}"
    echo "2   EMAIL     86400 1110 UTF8 test_hrls_${i}@test_hrls_check.com"
    echo "3   TEXT      86400 1110 UTF8 This handle is used to check if the hrls is functioning"
    echo "100 HS_ADMIN  86400 1110 ADMIN 200:110011111110:0.NA/${PREFIX}"
    if [[ ${i} =~ "00000" ]]
    then
       echo  "333 HS_SECKEY 86400 1100 UTF8 my_HS_SECKEY_string"
    fi
    echo ""
done 
