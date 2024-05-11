/**
	Group Signature Crypto Library
	Copyright (C) 2010 - Diego Ferri

	This file is part of Group Signature Crypto Library.

    Group Signature Crypto Library is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Group Signature Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Group Signature Crypto Library.  If not, see <http://www.gnu.org/licenses/>
*/

package it.unibs.gs.acjt.certs;

import java.math.BigInteger;
import java.util.Date;

import it.unibs.gs.acjt.ACJTMemberKeyImpl;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.interfaces.GSPrivateKey;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

public class ACJTMemberCertificate extends GSMemberCertificate {
    /**
     * 
     */
    private static final long serialVersionUID = -6214613347059704700L;

    public ACJTMemberCertificate(String algorithm, float version, String issuer, String subject, Date startDate,
            Date expiryDate, GSSecurityParameters params, GSPrivateKey key) {
        super(algorithm, version, issuer, subject, startDate, expiryDate, params, key);
    }

    @Override
    public ACJTMemberKeyImpl getMemberKey() {
        return (ACJTMemberKeyImpl) key;
    }

    @Override
    public byte[] getEncodedForSigning() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addSignature(byte[] signature) {
        // TODO Auto-generated method stub

    }

    @Override
    public BigInteger getSerialNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GSPublicKey getGPK() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getSignature() {
        // TODO Auto-generated method stub
        return null;
    }
}
