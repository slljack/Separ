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

package it.unibs.gs.acjt;

import it.unibs.gs.acjt.interfaces.ACJTPrivateKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;


public final class ACJTGroupPrivateKeyImpl implements ACJTPrivateKey
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4580333057480092177L;
	private BigInteger p_, q_, x;
	
	public ACJTGroupPrivateKeyImpl(BigInteger p_, 
			BigInteger q_, BigInteger x)
	{
		this.p_ = p_;
		this.q_ = q_;
		this.x = x;
	}
	
	public ACJTGroupPrivateKeyImpl(byte[] encoded)
	{
		ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));
		
		DERSequence s = null;
		try
		{
			s = (DERSequence)dIn.readObject();
		}
		catch (IOException e)
		{
			throw new SecurityException("can't decode Private Key object");
		}
		
		this.p_ = ((DERInteger)s.getObjectAt(0)).getValue();
		this.q_ = ((DERInteger)s.getObjectAt(1)).getValue();
		this.x = ((DERInteger)s.getObjectAt(2)).getValue();
	}
	
	public String toString()
	{
		return "ACJT Group Private Key (" + getSize() + "bits)" +
			"\np_ (" + p_.bitLength() + "bit): "  + p_ + 
			"\nq_ (" + q_.bitLength() + "bit): " + q_ +
			"\nx (" + x.bitLength() + "bit): " + x;
	}

	public BigInteger getP_()
	{
		return p_;
	}

	public BigInteger getQ_()
	{
		return q_;
	}

	public BigInteger getX()
	{
		return x;
	}
	
	public int getSize()
	{
		return p_.bitLength() + q_.bitLength() + x.bitLength();
	}

	public byte[] getEncoded()
	{
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERInteger(getP_()));
		v.add(new DERInteger(getQ_()));
		v.add(new DERInteger(getX()));

		DERSequence s = new DERSequence(v);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		try
		{
			dOut.writeObject(s);
			dOut.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bOut.toByteArray();
	}


	public String getFormat()
	{
		return "ASN.1/DER Format";
	}
	
	public String getAlgorithm()
	{
		return "ACJT - Ateniese Camenisch Joye Tsudik Group Signature Scheme";
	}

}
