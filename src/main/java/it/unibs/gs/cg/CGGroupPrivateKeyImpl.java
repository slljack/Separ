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

import it.unibs.gs.cg.interfaces.CGPrivateKey;

public final class CGGroupPrivateKeyImpl implements CGPrivateKey {
    /**
     *
     */
    private static final long serialVersionUID = -5833441178944326592L;

    private BigInteger p, q, XG;

    public CGGroupPrivateKeyImpl(BigInteger p, BigInteger q, BigInteger XG) {
        this.p = p;
        this.q = q;
        this.XG = XG;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getXG() {
        return XG;
    }

    public CGGroupPrivateKeyImpl(byte[] encoded) {
        ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));

        DLSequence s = null;
        try {
            s = (DLSequence) dIn.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode Private Key object");
        }

        this.p = ((ASN1Integer) s.getObjectAt(0)).getValue();
        this.q = ((ASN1Integer) s.getObjectAt(1)).getValue();
        this.XG = ((ASN1Integer) s.getObjectAt(2)).getValue();
    }

    @Override
    public String toString() {
        return "CG Group Private Key (" + getSize() + "bits)" + "\np (" + p.bitLength() + "bit): " + p + "\nq ("
                + q.bitLength() + "bit): " + q + "\nXg (" + XG.bitLength() + "bit): " + XG;
    }

    @Override
    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(p));
        v.add(new ASN1Integer(p));
        v.add(new ASN1Integer(XG));

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
    public int getSize() {
        return p.bitLength() + q.bitLength() + XG.bitLength();
    }

    @Override
    public String getAlgorithm() {
        return "CG - Camenisch Groth Group Signature Scheme";
    }
}
