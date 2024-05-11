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

package it.unibs.gs;

import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.interfaces.GSIdentity;
import it.unibs.gs.interfaces.GSKeyPair;
import it.unibs.gs.interfaces.GSPrivateKey;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Vector;


public abstract class GSGroupManagerSpi
{
	protected SecureRandom random;
	protected Vector<GSIdentity> members;
	
	protected abstract GSKeyPair engineDoSetup(GSSecurityParameters params) throws NoSuchAlgorithmException;
	protected abstract GSGroupCertificate engineBuildGroupCertificate(GSPublicKey gpk, String issuer, String subject, 
			Date startDate, Date expiryDate, BigInteger serialNumber, GSSecurityParameters params);
	protected abstract GSMemberCertificate engineDoJoin(String subject, GSGroupCertificate groupCert, GSPrivateKey gsk);
	protected abstract GSPublicKey engineDoRevoke(GSPublicKey gpk, GSIdentity identity) throws Exception;
	protected abstract void engineDoFullRevoke(GSIdentity identity) throws Exception;
	//protected abstract void engineDestroy();
	//protected abstract void engineSaveState();
	//protected abstract void engineLoadState();
	//protected abstract void engineDoAction(String action, Object [] objs);
}
