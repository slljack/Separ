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

import it.unibs.gs.interfaces.GSIdentity;
import it.unibs.gs.interfaces.GSPrivateKey;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSRevocationList;
import it.unibs.gs.interfaces.GSSecurityParameters;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;



public abstract class GSSignatureSpi
{
	protected enum State {UNINITIALIZED, SIGN, VERIFY, OPEN, REVOKE};
    /**
     * Application-specified source of randomness. 
     */
    protected SecureRandom random = null;

    protected abstract void engineInitSign(GSPublicKey gpk, GSPrivateKey mk, GSSecurityParameters params, SecureRandom random)
		throws InvalidKeyException;

    protected abstract void engineInitVerify(GSPublicKey gpk, GSSecurityParameters params)
		throws InvalidKeyException;

    protected abstract void engineInitOpen(GSPublicKey gpk, GSPrivateKey gsk)
		throws InvalidKeyException;
    
    protected abstract void engineInitRevoke(GSPublicKey gpk, GSRevocationList list)
    	throws InvalidKeyException;
    
    protected abstract void engineUpdate(byte[] b, int off, int len) 
        throws SignatureException;

    protected void engineUpdate(ByteBuffer input) 
    {
    	if (input.hasRemaining() == false) 
    	{
    		return;
    	}
    	try 
    	{
    		if (input.hasArray()) 
    		{
				byte[] b = input.array();
				int ofs = input.arrayOffset();
				int pos = input.position();
				int lim = input.limit();
				engineUpdate(b, ofs + pos, lim - pos);
				input.position(lim);
			    
    		} 
    		else 
    		{
				int len = input.remaining();
				byte[] b = new byte[len];
				while (len > 0) 
				{
				    int chunk = Math.min(len, b.length);
				    input.get(b, 0, chunk);
				    engineUpdate(b, 0, chunk);
				    len -= chunk;
				}
    		}
    	} 
    	catch (SignatureException e) 
    	{
    		throw new ProviderException("update() failed", e);
    	}
    }

    protected abstract byte[] engineSign() throws SignatureException;


    protected int engineSign(byte[] outbuf, int offset, int len)
	                throws SignatureException 
	{
    	byte[] sig = engineSign();
    	if (len < sig.length) 
    	{
    		throw new SignatureException
		    	("partial signatures not returned");
    	}
    	if (outbuf.length - offset < sig.length) 
    	{
    		throw new SignatureException
		    	("insufficient space in the output buffer to store the "
		    			+ "signature");
    	}
    	System.arraycopy(sig, 0, outbuf, offset, sig.length);
    	return sig.length;
    }

    protected abstract boolean engineVerify(byte[] sigBytes) 
		throws SignatureException;

    protected abstract GSIdentity engineOpen(byte[] sigBytes) 
		throws SignatureException;
    
    protected abstract boolean engineRevoke(byte[] sigBytes)
    	throws SignatureException;


    protected boolean engineVerify(byte[] sigBytes, int offset, int length) 
		throws SignatureException 
	{
		byte[] sigBytesCopy = new byte[length];
		System.arraycopy(sigBytes, offset, sigBytesCopy, 0, length);
		return engineVerify(sigBytesCopy);
    }

}
