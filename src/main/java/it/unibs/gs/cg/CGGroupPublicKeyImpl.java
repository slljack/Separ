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

import it.unibs.gs.cg.interfaces.CGPublicKey;

public final class CGGroupPublicKeyImpl implements CGPublicKey {
    /**
     *
     */
    private static final long serialVersionUID = 2384916427843471231L;
    private BigInteger n, a, g, h, Q, P, F, G, H, w, f;

    public CGGroupPublicKeyImpl(BigInteger n, BigInteger a, BigInteger g, BigInteger h, BigInteger Q, BigInteger P,
            BigInteger F, BigInteger G, BigInteger H, BigInteger w, BigInteger f) {
        this.n = n;
        this.a = a;
        this.g = g;
        this.h = h;
        this.Q = Q;
        this.P = P;
        this.F = F;
        this.G = G;
        this.H = H;
        this.w = w;
        this.f = f;
    }

    public CGGroupPublicKeyImpl(byte[] encoded) {
        ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));

        DLSequence s = null;
        try {
            s = (DLSequence) dIn.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode Public Key object");
        }

        this.n = ((ASN1Integer) s.getObjectAt(0)).getValue();
        this.a = ((ASN1Integer) s.getObjectAt(1)).getValue();
        this.g = ((ASN1Integer) s.getObjectAt(2)).getValue();
        this.h = ((ASN1Integer) s.getObjectAt(3)).getValue();
        this.Q = ((ASN1Integer) s.getObjectAt(4)).getValue();
        this.P = ((ASN1Integer) s.getObjectAt(5)).getValue();
        this.F = ((ASN1Integer) s.getObjectAt(6)).getValue();
        this.G = ((ASN1Integer) s.getObjectAt(7)).getValue();
        this.H = ((ASN1Integer) s.getObjectAt(8)).getValue();
        this.w = ((ASN1Integer) s.getObjectAt(9)).getValue();
        this.f = ((ASN1Integer) s.getObjectAt(10)).getValue();
    }

    @Override
    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(n));
        v.add(new ASN1Integer(a));
        v.add(new ASN1Integer(g));
        v.add(new ASN1Integer(h));
        v.add(new ASN1Integer(Q));
        v.add(new ASN1Integer(P));
        v.add(new ASN1Integer(F));
        v.add(new ASN1Integer(G));
        v.add(new ASN1Integer(H));
        v.add(new ASN1Integer(w));
        v.add(new ASN1Integer(f));

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
        return n.bitLength() + a.bitLength() + g.bitLength() + h.bitLength() + Q.bitLength() + P.bitLength()
                + F.bitLength() + G.bitLength() + H.bitLength() + w.bitLength() + f.bitLength();
    }

    @Override
    public String toString() {
        return "CG Group Public Key (" + getSize() + "bits)" + "\nn (" + n.bitLength() + "bit): " + n + "\na ("
                + a.bitLength() + "bit): " + a + "\ng (" + g.bitLength() + "bit): " + g + "\nh (" + h.bitLength()
                + "bit): " + h + "\nQ (" + Q.bitLength() + "bit): " + Q + "\nP (" + P.bitLength() + "bit): " + P
                + "\nF (" + F.bitLength() + "bit): " + F + "\nG (" + G.bitLength() + "bit): " + G + "\nH ("
                + H.bitLength() + "bit): " + H + "\nw (" + w.bitLength() + "bit): " + w + "\nf (" + f.bitLength()
                + "bit): " + f;
    }

    @Override
    public String getAlgorithm() {
        return "CG - Camenisch Groth Group Signature Scheme";
    }

    public BigInteger getn() {
        return n;
    }

    public BigInteger geta() {
        return a;
    }

    public BigInteger getg() {
        return g;
    }

    public BigInteger geth() {
        return h;
    }

    public BigInteger getQ() {
        return Q;
    }

    public BigInteger getP() {
        return P;
    }

    public BigInteger getF() {
        return F;
    }

    public BigInteger getG() {
        return G;
    }

    public BigInteger getH() {
        return H;
    }

    public BigInteger getw() {
        return w;
    }

    public BigInteger getf() {
        return f;
    }

}
