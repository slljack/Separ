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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLOutputStream;
import org.bouncycastle.asn1.DLSequence;

import it.unibs.gs.acjt.interfaces.ACJTPrivateKey;

public class CGMemberKeyImpl implements ACJTPrivateKey {
    /**
     *
     */
    private static final long serialVersionUID = -4673431540209243013L;
    private BigInteger w, x, r, y, e, s;

    public CGMemberKeyImpl(BigInteger w, BigInteger x, BigInteger r, BigInteger y, BigInteger e, BigInteger s) {
        this.w = w;
        this.x = x;
        this.r = r;
        this.y = y;
        this.e = e;
        this.s = s;
    }

    public CGMemberKeyImpl(byte[] encoded) {
        ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));

        DLSequence s = null;
        try {
            s = (DLSequence) dIn.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode Certificate object");
        }

        this.w = ((ASN1Integer) s.getObjectAt(0)).getValue();
        this.x = ((ASN1Integer) s.getObjectAt(1)).getValue();
        this.r = ((ASN1Integer) s.getObjectAt(2)).getValue();
        this.y = ((ASN1Integer) s.getObjectAt(3)).getValue();
        this.e = ((ASN1Integer) s.getObjectAt(4)).getValue();
        this.s = ((ASN1Integer) s.getObjectAt(5)).getValue();
    }

    public BigInteger getw() {
        return w;
    }

    public BigInteger getx() {
        return x;
    }

    public BigInteger getr() {
        return r;
    }

    public BigInteger gety() {
        return y;
    }

    public BigInteger gete() {
        return e;
    }

    public BigInteger gets() {
        return s;
    }

    @Override
    public int getSize() {
        return w.bitLength() + x.bitLength() + r.bitLength() + y.bitLength() + e.bitLength() + s.bitLength();
    }

    @Override
    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(w));
        v.add(new ASN1Integer(x));
        v.add(new ASN1Integer(r));
        v.add(new ASN1Integer(y));
        v.add(new ASN1Integer(e));
        v.add(new ASN1Integer(s));

        DLSequence s = new DLSequence(v);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DLOutputStream dOut = new DLOutputStream(bOut);
        try {
            dOut.writeObject(s);
            dOut.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bOut.toByteArray();
    }

    @Override
    public String getFormat() {
        return "ASN.1/DER Format";
    }

    @Override
    public String getAlgorithm() {
        return "CG - Camenisch Groth Group Signature Scheme";
    }
}
