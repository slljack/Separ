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

package it.unibs.gs.cg;

import java.security.Provider;

public class CGProvider extends Provider
{
	private static final long serialVersionUID = 3780608368753039174L;
	private static final String NAME = "CG";
	private static final double VERSION = 2.0;
	private static final String INFO = "CG Group Signature Provider by Diego Ferri 2010";
	
	public CGProvider()
	{
		super(CGProvider.NAME, CGProvider.VERSION, CGProvider.INFO);
		put("KeyPairGenerator.CG", "it.unibs.gs.cg.CGGroupKeyPairGenerator");
		put("GSSignature.CG", "it.unibs.gs.cg.CGSignature");
		put("GSGroupManager.CG", "it.unibs.gs.cg.CGGroupManager");
	}

}
