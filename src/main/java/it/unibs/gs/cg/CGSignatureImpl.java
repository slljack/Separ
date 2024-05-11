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

public class CGSignatureImpl {
    /**
     *
     */
    private static final long serialVersionUID = -3606380613494472186L;
    private BigInteger c, u, U1, U2, U3, zx, zr, ze, ZR;

    public CGSignatureImpl(BigInteger c, BigInteger u, BigInteger U1, BigInteger U2, BigInteger U3, BigInteger zx,
            BigInteger zr, BigInteger ze, BigInteger ZR) {
        this.c = c;
        this.u = u;
        this.U1 = U1;
        this.U2 = U2;
        this.U3 = U3;
        this.zx = zx;
        this.zr = zr;
        this.ze = ze;
        this.ZR = ZR;
    }

    public CGSignatureImpl(byte[] encoded) {
        ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));

        DLSequence s = null;
        try {
            s = (DLSequence) dIn.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode Certificate object");
        }

        this.c = ((ASN1Integer) s.getObjectAt(0)).getValue();
        this.u = ((ASN1Integer) s.getObjectAt(1)).getValue();
        this.U1 = ((ASN1Integer) s.getObjectAt(2)).getValue();
        this.U2 = ((ASN1Integer) s.getObjectAt(3)).getValue();
        this.U3 = ((ASN1Integer) s.getObjectAt(4)).getValue();
        this.zx = ((ASN1Integer) s.getObjectAt(5)).getValue();
        this.zr = ((ASN1Integer) s.getObjectAt(6)).getValue();
        this.ze = ((ASN1Integer) s.getObjectAt(7)).getValue();
        this.ZR = ((ASN1Integer) s.getObjectAt(8)).getValue();
    }

    @Override
    public String toString() {
        return "CG Signature (" + getSize() + "bits)" + "\nc (" + c.bitLength() + "bit): " + c + "\nu (" + u.bitLength()
                + "bit): " + u + "\nU1 (" + U1.bitLength() + "bit): " + U1 + "\nU2 (" + U2.bitLength() + "bit): " + U2
                + "\nU3 (" + U3.bitLength() + "bit): " + U3 +
                // "\nU4 (" + U4.bitLength() + "bit): " + U4 +
                "\nzx (" + zx.bitLength() + "bit): " + zx + "\nzr (" + zr.bitLength() + "bit): " + zr + "\nze ("
                + ze.bitLength() + "bit): " + ze +
                // "\nzs (" + zs.bitLength() + "bit): " + zs +
                "\nZR (" + ZR.bitLength() + "bit): " + ZR;

    }

    public int getSize() {
        return c.bitLength() + u.bitLength() + U1.bitLength() + U2.bitLength() + U3.bitLength() + zx.bitLength()
                + zr.bitLength() + ze.bitLength() + ZR.bitLength();
    }

    public BigInteger getc() {
        return c;
    }

    public BigInteger getu() {
        return u;
    }

    public BigInteger getU1() {
        return U1;
    }

    public BigInteger getU2() {
        return U2;
    }

    public BigInteger getU3() {
        return U3;
    }

    public BigInteger getzx() {
        return zx;
    }

    public BigInteger getzr() {
        return zr;
    }

    public BigInteger getze() {
        return ze;
    }

    public BigInteger getZR() {
        return ZR;
    }

    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(c));
        v.add(new ASN1Integer(u));
        v.add(new ASN1Integer(U1));
        v.add(new ASN1Integer(U2));
        v.add(new ASN1Integer(U3));
        // v.add(new ASN1Integer(U4));
        v.add(new ASN1Integer(zx));
        v.add(new ASN1Integer(zr));
        v.add(new ASN1Integer(ze));
        // v.add(new ASN1Integer(zs));
        v.add(new ASN1Integer(ZR));

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

    public String getFormat() {
        return "ASN.1/DER Format";
    }

    public String getAlgorithm() {
        return "CG - Camenisch Groth Group Signature Scheme";
    }
}
