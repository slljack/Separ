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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.Provider.Service;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;


public abstract class GSSignature extends GSSignatureSpi
{
	private String algorithm;

	// The provider
	Provider provider;

	/**
	 * Current state of this signature object.
	 */
	protected State currentState = State.UNINITIALIZED;
	

	protected GSSignature(String algorithm)
	{
		this.algorithm = algorithm;
	}

	public static GSSignature getInstance(String algorithm)
			throws NoSuchAlgorithmException
	{
		List<Service> list;
		list = GetInstance.getServices("GSSignature", algorithm);

		Iterator<Service> t = list.iterator();
		if (t.hasNext() == false)
		{
			throw new NoSuchAlgorithmException(algorithm
					+ " GSignature not available");
		}
		// try services until we find an Spi or a working Signature subclass
		NoSuchAlgorithmException failure;
		do
		{
			Service s = (Service) t.next();
			if (isSpi(s))
			{
				return new Delegate(s, t, algorithm);
			}
			else
			{
				// must be a subclass of Signature, disable dynamic selection
				try
				{
					Instance instance = GetInstance.getInstance(s,
							GSSignatureSpi.class);
					return getInstance(instance, algorithm);
				}
				catch (NoSuchAlgorithmException e)
				{
					failure = e;
				}
			}
		}
		while (t.hasNext());
		throw failure;
	}

	private static GSSignature getInstance(Instance instance, String algorithm)
	{
		GSSignature sig;
		if (instance.impl instanceof GSSignature)
		{
			sig = (GSSignature) instance.impl;
		}
		else
		{
			GSSignatureSpi spi = (GSSignatureSpi) instance.impl;
			sig = new Delegate(spi, algorithm);
		}
		sig.provider = instance.provider;
		return sig;
	}

	private final static Map<String, Boolean> signatureInfo;

	static
	{
		signatureInfo = new ConcurrentHashMap<String, Boolean>();
	}

	private static boolean isSpi(Service s)
	{
		if (s.getType().equals("GSSignature"))
		{
			// must be a GSignatureSpi, which we can wrap with the GSignatureAdapter
			return true;
		}
		String className = s.getClassName();
		Boolean result = signatureInfo.get(className);
		if (result == null)
		{
			try
			{
				Object instance = s.newInstance(null);
				// GSSignature extends GSSignatureSpi
				// so it is a "real" Spi if it is an
				// instance of SignatureSpi but not Signature
				boolean r = (instance instanceof GSSignatureSpi)
						&& (instance instanceof GSSignature == false);
				
				result = Boolean.valueOf(r);
				signatureInfo.put(className, result);
			}
			catch (Exception e)
			{
				// something is wrong, assume not an SPI
				return false;
			}
		}
		return result.booleanValue();
	}

	public static GSSignature getInstance(String algorithm, String provider)
			throws NoSuchAlgorithmException, NoSuchProviderException
	{
		Instance instance = GetInstance.getInstance("GSSignature",
				GSSignatureSpi.class, algorithm, provider);
		return getInstance(instance, algorithm);
	}

	
	public static GSSignature getInstance(String algorithm, Provider provider)
			throws NoSuchAlgorithmException
	{
		Instance instance = GetInstance.getInstance("GSSignature",
				GSSignatureSpi.class, algorithm, provider);
		return getInstance(instance, algorithm);
	}


	/**
	 * Returns the provider of this signature object.
	 * 
	 * @return the provider of this signature object
	 */
	public final Provider getProvider()
	{
		chooseFirstProvider();
		return this.provider;
	}

	void chooseFirstProvider()
	{
		// empty, overridden in Delegate
	}

	public final void initOpen(GSPublicKey gpk, GSPrivateKey gsk)
			throws InvalidKeyException
	{
		engineInitOpen(gpk, gsk);
		currentState = State.OPEN;
	}
	
	public final void initRevoke(GSPublicKey gpk, GSRevocationList list)
			throws InvalidKeyException
	{
		engineInitRevoke(gpk, list);
		currentState = State.REVOKE;
	}
	
	public final void initVerify(GSPublicKey gpk, GSSecurityParameters params)
			throws InvalidKeyException
	{
		engineInitVerify(gpk, params);
		currentState = State.VERIFY;
	}

	public final void initSign(GSPublicKey gpk, GSPrivateKey mk, GSSecurityParameters params, SecureRandom random)
			throws InvalidKeyException
	{
		engineInitSign(gpk, mk, params, random);
		currentState = State.SIGN;
	}
	
	public final byte[] sign() throws SignatureException
	{
		if (currentState == State.SIGN)
		{
			return engineSign();
		}
		throw new SignatureException("object not initialized for " + "signing");
	}


	public final int sign(byte[] outbuf, int offset, int len)
			throws SignatureException
	{
		if (outbuf == null)
		{
			throw new IllegalArgumentException("No output buffer given");
		}
		if (outbuf.length - offset < len)
		{
			throw new IllegalArgumentException(
					"Output buffer too small for specified offset and length");
		}
		if (currentState != State.SIGN)
		{
			throw new SignatureException("object not initialized for "
					+ "signing");
		}
		return engineSign(outbuf, offset, len);
	}
	
	public final GSIdentity open(byte[] signature) throws SignatureException
	{
		if (currentState == State.OPEN)
		{
			return engineOpen(signature);
		}
		throw new SignatureException("object not initialized for "
				+ "opening");
	}
	
	public final boolean revoke(byte[] signature) throws SignatureException
	{
		if (currentState == State.REVOKE)
		{
			return engineRevoke(signature);
		}
		throw new SignatureException("object not initialized for "
				+ "revoking");		
	}
	
	public final boolean verify(byte[] signature) throws SignatureException
	{
		if (currentState == State.VERIFY)
		{
			return engineVerify(signature);
		}
		throw new SignatureException("object not initialized for "
				+ "verification");
	}
	
	
	public final boolean verify(byte[] signature, int offset, int length)
			throws SignatureException
	{
		if (currentState == State.VERIFY)
		{
			if ((signature == null) || (offset < 0) || (length < 0)
					|| (offset + length > signature.length))
			{
				throw new IllegalArgumentException("Bad arguments");
			}

			return engineVerify(signature, offset, length);
		}
		throw new SignatureException("object not initialized for "
				+ "verification");
	}

	public final void update(byte[] data) throws SignatureException
	{
		update(data, 0, data.length);
	}

	
	public final void update(byte[] data, int off, int len)
			throws SignatureException
	{
		if (currentState != State.UNINITIALIZED)
		{
			engineUpdate(data, off, len);
		}
		else
		{
			throw new SignatureException("object not initialized for "
					+ "signature, verification, opening");
		}
	}

	public final void update(ByteBuffer data) throws SignatureException
	{
		if (currentState != State.UNINITIALIZED)
		{
			throw new SignatureException("object not initialized for "
					+ "signature or verification");
		}
		if (data == null)
		{
			throw new NullPointerException();
		}
		engineUpdate(data);
	}

	public final String getAlgorithm()
	{
		return this.algorithm;
	}

	public String toString()
	{
		String initState = "";
		switch (currentState)
		{
		case UNINITIALIZED:
			initState = "<not initialized>";
			break;
		case VERIFY:
			initState = "<initialized for verifying>";
			break;
		case SIGN:
			initState = "<initialized for signing>";
			break;
		case OPEN:
			initState = "<initialized for opening>";
			break;
		case REVOKE:
			initState = "<initialized for revoking>";
			break;
		}
		return "Signature object: " + getAlgorithm() + initState;
	}

	private static class Delegate extends GSSignature
	{

		// The provider implementation (delegate)
		// filled in once the provider is selected
		private GSSignatureSpi sigSpi;

		// lock for mutex during provider selection
		private final Object lock;

		// next service to try in provider selection
		// null once provider is selected
		private Service firstService;

		// remaining services to try in provider selection
		// null once provider is selected
		private Iterator<Service> serviceIterator;

		// constructor
		Delegate(GSSignatureSpi sigSpi, String algorithm)
		{
			super(algorithm);
			this.sigSpi = sigSpi;
			this.lock = null; // no lock needed
		}

		// used with delayed provider selection
		Delegate(Service service, Iterator<Service> iterator, String algorithm)
		{
			super(algorithm);
			this.firstService = service;
			this.serviceIterator = iterator;
			this.lock = new Object();
		}

		
		private static GSSignatureSpi newInstance(Service s)
				throws NoSuchAlgorithmException
		{
			Object o = s.newInstance(null);
			if (o instanceof GSSignatureSpi == false)
			{
				throw new NoSuchAlgorithmException("Not a GSSignatureSpi: "
						+ o.getClass().getName());
			}
			return (GSSignatureSpi) o;
		}

		/**
		 * Choose the Spi from the first provider available. Used if delayed
		 * provider selection is not possible because initSign()/ initVerify()
		 * is not the first method called.
		 */
		void chooseFirstProvider()
		{
			if (sigSpi != null)
			{
				return;
			}
			synchronized (lock)
			{
				if (sigSpi != null)
				{
					return;
				}

				Exception lastException = null;
				while ((firstService != null) || serviceIterator.hasNext())
				{
					Service s;
					if (firstService != null)
					{
						s = firstService;
						firstService = null;
					}
					else
					{
						s = (Service) serviceIterator.next();
					}
					if (isSpi(s) == false)
					{
						continue;
					}
					try
					{
						sigSpi = newInstance(s);
						provider = s.getProvider();
						// not needed any more
						firstService = null;
						serviceIterator = null;
						return;
					}
					catch (NoSuchAlgorithmException e)
					{
						lastException = e;
					}
				}
				ProviderException e = new ProviderException(
						"Could not construct GSSignatureSpi instance");
				if (lastException != null)
				{
					e.initCause(lastException);
				}
				throw e;
			}
		}

		private void chooseProvider(State state, Object[] objs, SecureRandom random)
				throws InvalidKeyException
		{
			synchronized (lock)
			{
				if (sigSpi != null)
				{
					init(sigSpi, state, objs, random);
					return;
				}
				Exception lastException = null;
				while ((firstService != null) || serviceIterator.hasNext())
				{
					Service s;
					if (firstService != null)
					{
						s = firstService;
						firstService = null;
					}
					else
					{
						s = (Service) serviceIterator.next();
					}
					// if provider says it does not support this key, ignore it
					if (s.supportsParameter(objs) == false)
					{
						continue;
					}
					// if instance is not a GSSignatureSpi, ignore it
					if (isSpi(s) == false)
					{
						continue;
					}
					try
					{
						GSSignatureSpi spi = newInstance(s);
						init(spi, state, objs, random);
						provider = s.getProvider();
						sigSpi = spi;
						firstService = null;
						serviceIterator = null;
						return;
					}
					catch (Exception e)
					{
						// NoSuchAlgorithmException from newInstance()
						// InvalidKeyException from init()
						// RuntimeException (ProviderException) from init()
						if (lastException == null)
						{
							lastException = e;
						}
					}
				}
				// no working provider found, fail
				if (lastException instanceof InvalidKeyException)
				{
					throw (InvalidKeyException) lastException;
				}
				if (lastException instanceof RuntimeException)
				{
					throw (RuntimeException) lastException;
				}
			
				throw new InvalidKeyException(
						"No installed provider supports this operation.",
						lastException);
			}
		}
		
		private void init(GSSignatureSpi spi, State state, Object [] objs, 
				SecureRandom random) throws InvalidKeyException
		{
			switch (state)
			{
			case SIGN:
			{
				GSPublicKey gpk = (GSPublicKey) objs[0];
				GSPrivateKey mk = (GSPrivateKey) objs[1];
				GSSecurityParameters params = (GSSecurityParameters) objs[2];
				spi.engineInitSign(gpk, mk, params, random);
				break;
			}
			case VERIFY:
			{
				GSPublicKey gpk = (GSPublicKey)objs[0];
				GSSecurityParameters params = (GSSecurityParameters) objs[1];
				spi.engineInitVerify(gpk, params);
				break;
			}
			
			case OPEN:
			{
				GSPublicKey gpk = (GSPublicKey)objs[0];
				GSPrivateKey gsk = (GSPrivateKey)objs[1];
				spi.engineInitOpen(gpk, gsk);
				break;
			}
			default:
				throw new AssertionError("Internal error: " + state);
			}
		}

		protected void engineInitVerify(GSPublicKey gpk, GSSecurityParameters params)
				throws InvalidKeyException
		{
			if (sigSpi != null)
			{
				sigSpi.engineInitVerify(gpk, params);
			}
			else
			{
				Object [] objs = new Object[]{gpk, params};
				chooseProvider(State.VERIFY, objs, null);
			}
		}
		
		protected void engineInitOpen(GSPublicKey gpk, GSPrivateKey gsk)
				throws InvalidKeyException
		{
			if (sigSpi != null)
			{
				sigSpi.engineInitOpen(gpk, gsk);
			}
			else
			{
				Object [] objs = new Object[]{gpk, gsk};
				chooseProvider(State.OPEN, objs, null);
			}
		}
		
		protected void engineInitRevoke(GSPublicKey gpk, GSRevocationList list)
			throws InvalidKeyException
		{
			if (sigSpi != null)
			{
				sigSpi.engineInitRevoke(gpk, list);
			}
			else
			{
				Object [] objs = new Object[]{gpk, list};
				chooseProvider(State.REVOKE, objs, null);
			}
		}

		protected void engineInitSign(GSPublicKey gpk, GSPrivateKey mk, GSSecurityParameters params, SecureRandom random)
				throws InvalidKeyException
		{
			if (sigSpi != null)
			{
				sigSpi.engineInitSign(gpk, mk, params, random);
			}
			else
			{
				Object [] objs = new Object[]{gpk, mk, params, random};
				chooseProvider(State.SIGN, objs, random);
			}
		}
		
		protected void engineUpdate(byte[] b, int off, int len)
				throws SignatureException
		{
			chooseFirstProvider();
			sigSpi.engineUpdate(b, off, len);
		}

		protected void engineUpdate(ByteBuffer data)
		{
			chooseFirstProvider();
			sigSpi.engineUpdate(data);
		}

		protected byte[] engineSign() 
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineSign();
		}

		protected int engineSign(byte[] outbuf, int offset, int len)
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineSign(outbuf, offset, len);
		}

		protected boolean engineVerify(byte[] sigBytes)
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineVerify(sigBytes);
		}

		protected boolean engineVerify(byte[] sigBytes, int offset, int length)
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineVerify(sigBytes, offset, length);
		}
		
		protected GSIdentity engineOpen(byte[] sigBytes)
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineOpen(sigBytes);
		}
		
		protected boolean engineRevoke(byte[] sigBytes)
			throws SignatureException
		{
			chooseFirstProvider();
			return sigSpi.engineRevoke(sigBytes);
		}
	}
}
