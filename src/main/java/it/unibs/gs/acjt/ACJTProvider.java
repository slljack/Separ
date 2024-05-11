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

import java.security.Provider;

public class ACJTProvider extends Provider
{
	private static final long serialVersionUID = 3780608368753039174L;
	private static final String NAME = "ACJT";
	private static final double VERSION = 2.0;
	private static final String INFO = "ACJT Group Signature Provider by Diego Ferri 2010";
	
	public ACJTProvider()
	{
		super(ACJTProvider.NAME, ACJTProvider.VERSION, ACJTProvider.INFO);
		put("KeyPairGenerator.ACJT", "it.unibs.gs.acjt.ACJTGroupKeyPairGenerator");
		put("GSSignature.ACJT", "it.unibs.gs.acjt.ACJTSignature");
		put("GSGroupManager.ACJT", "it.unibs.gs.acjt.ACJTGroupManager");
	}
}
