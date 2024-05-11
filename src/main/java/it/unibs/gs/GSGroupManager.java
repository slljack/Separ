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
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Provider.Service;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;


public abstract class GSGroupManager extends GSGroupManagerSpi
{
	private String algorithm;
	Provider provider;
	
	protected GSGroupManager(String algorithm)
	{
		this.algorithm = algorithm;
	}
	
	public static GSGroupManager getInstance(String algorithm)
		throws NoSuchAlgorithmException
	{
		List<Service> list;
		list = GetInstance.getServices("GSGroupManager", algorithm);
	
		Iterator<Service> t = list.iterator();
		if (t.hasNext() == false)
		{
			throw new NoSuchAlgorithmException(algorithm
					+ " GSGroupManager not available");
		}
		// try services until we find an Spi or a working Factory subclass
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
				// must be a subclass of Factory, disable dynamic selection
				try
				{
					Instance instance = GetInstance.getInstance(s,
							GSGroupManager.class);
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
	
	private static GSGroupManager getInstance(Instance instance, String algorithm)
	{
		GSGroupManager sig;
		if (instance.impl instanceof GSGroupManager)
		{
			sig = (GSGroupManager) instance.impl;
		}
		else
		{
			GSGroupManagerSpi spi = (GSGroupManagerSpi) instance.impl;
			sig = new Delegate(spi, algorithm);
		}
		sig.provider = instance.provider;
		return sig;
	}
	
	private final static Map<String, Boolean> managerInfo;

	static
	{
		managerInfo = new ConcurrentHashMap<String, Boolean>();
	}
	
	private static boolean isSpi(Service s)
	{
		if (s.getType().equals("GSGroupManager"))
		{
			// must be a GSGroupManagerSpi, which we can wrap with the GSUserFactoryAdapter
			return true;
		}
		String className = s.getClassName();
		Boolean result = managerInfo.get(className);
		if (result == null)
		{
			try
			{
				Object instance = s.newInstance(null);
				// GSUserFactory extends GSUserFactorySpi
				// so it is a "real" Spi if it is an
				// instance of GSUserFactoryeSpi but not GSUserFactory
				boolean r = (instance instanceof GSGroupManagerSpi)
						&& (instance instanceof GSGroupManager == false);
				result = Boolean.valueOf(r);
				managerInfo.put(className, result);
			}
			catch (Exception e)
			{
				// something is wrong, assume not an SPI
				return false;
			}
		}
		return result.booleanValue();
	}
	
	
	/*public final void doAction(String action, Object [] objs)
	{
		engineDoAction(action, objs);
	}*/
	
	public final GSKeyPair doSetup(GSSecurityParameters params) throws NoSuchAlgorithmException
	{
		return engineDoSetup(params);
	}
	
	public final GSGroupCertificate buildGroupCertificate(GSPublicKey gpk, String issuer, String group, 
			Date startDate, Date expiryDate, BigInteger serialNumber, GSSecurityParameters params)
	{
		return engineBuildGroupCertificate(gpk, issuer, 
				group, startDate, expiryDate, serialNumber, params);
	}
	
	public final GSMemberCertificate joinGroup(String name, GSGroupCertificate groupCert, GSPrivateKey gsk)
	{
		return engineDoJoin(name, groupCert, gsk);
	}
	
	public final GSPublicKey doRevoke(GSPublicKey gpk, GSIdentity identity) throws Exception
	{
		return engineDoRevoke(gpk, identity);
	}
	
	public final void doFullRevoke(GSIdentity identity) throws Exception
	{
		engineDoFullRevoke(identity);
	}
	
	private static class Delegate extends GSGroupManager
	{
		private GSGroupManagerSpi spi;

		// lock for mutex during provider selection
		private final Object lock;

		// next service to try in provider selection
		// null once provider is selected
		private Service firstService;

		// remaining services to try in provider selection
		// null once provider is selected
		private Iterator<Service> serviceIterator;
		
		Delegate(GSGroupManagerSpi spi, String algorithm)
		{
			super(algorithm);
			this.spi = spi;
			this.lock = null; // no lock needed
		}
		
		Delegate(Service service, Iterator<Service> iterator, String algorithm)
		{
			super(algorithm);
			this.firstService = service;
			this.serviceIterator = iterator;
			this.lock = new Object();
		}
		
		/*protected void engineDoAction(String action, Object [] objs)
		{
			//Object[] params = objs;
			try
			{
				Method method = getClass().getMethod(action, 
						new Class<?>[]);
				method.invoke(this, objs);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e.getMessage(), e);
			}
		}*/
		
		private static GSGroupManagerSpi newInstance(Service s)
				throws NoSuchAlgorithmException
		{
			Object o = s.newInstance(null);
			if (o instanceof GSGroupManagerSpi == false)
			{
				throw new NoSuchAlgorithmException("Not a GSGroupManagerSpi: "
						+ o.getClass().getName());
			}
			return (GSGroupManagerSpi) o;
		}
		
		void chooseFirstProvider()
		{
			if (spi != null)
			{
				return;
			}
			synchronized (lock)
			{
				if (spi != null)
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
						spi = newInstance(s);
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
						"Could not construct GSUserFactorySpi instance");
				if (lastException != null)
				{
					e.initCause(lastException);
				}
				throw e;
			}
		}
		
		protected GSKeyPair engineDoSetup(GSSecurityParameters params) throws NoSuchAlgorithmException
		{
			if (spi == null)
				chooseFirstProvider();
			
			if(spi.members != null)
				throw new AccessControlException("This action will destroy the group. Please use destroy() before that");
				
			return spi.engineDoSetup(params);
		}
		
		protected GSMemberCertificate engineDoJoin(String subject, GSGroupCertificate groupCert, GSPrivateKey gsk)
		{
			return spi.engineDoJoin(subject, groupCert, gsk);
		}

		protected GSGroupCertificate engineBuildGroupCertificate(GSPublicKey gpk, String issuer, String subject, 
				Date startDate, Date expiryDate, BigInteger serialNumber, GSSecurityParameters params)
		{
			return spi.engineBuildGroupCertificate(gpk, issuer, 
					subject, startDate, expiryDate, serialNumber, params);
		}
		
		protected GSPublicKey engineDoRevoke(GSPublicKey gpk, GSIdentity identity) throws Exception
		{
			return spi.engineDoRevoke(gpk, identity);
		}
		
		protected void engineDoFullRevoke(GSIdentity identity) throws Exception
		{
			spi.engineDoFullRevoke(identity);
		}
	}
}
