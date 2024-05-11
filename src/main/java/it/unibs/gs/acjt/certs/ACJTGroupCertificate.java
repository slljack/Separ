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

import it.unibs.gs.acjt.ACJTGroupPublicKeyImpl;
import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.math.BigInteger;
import java.util.Date;


public class ACJTGroupCertificate extends GSGroupCertificate
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -883278889928150033L;

	public ACJTGroupCertificate(String algorithm, float version,
			BigInteger serialNumber, String issuer, String subject,
			Date startDate, Date expiryDate, GSPublicKey gpk,
			GSSecurityParameters params)
	{
		super(algorithm, version, serialNumber, issuer, subject, startDate, expiryDate,
				gpk, params);
		// TODO Auto-generated constructor stub
	}
	
	
	
	public ACJTGroupPublicKeyImpl getGPK()
	{
		return (ACJTGroupPublicKeyImpl) gpk;
	}

}
