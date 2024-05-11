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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DLOutputStream;
import org.bouncycastle.asn1.DLSequence;

import it.unibs.gs.interfaces.GSSecurityParameters;

public class CGSecurityParameters implements GSSecurityParameters {
    private int ln;
    private int lP;
    private int lE;
    private int lQ;
    private int lc;
    private int le;
    private int ls;
    private String hash;

    /**
     * Rules lc + le + ls + 1 < lQ lQ + lc + ls +1 < lE < ln/2 Suggestions
     *
     * @param ln 2048
     * @param lP 2048
     * @param lE 504
     * @param lQ 282
     * @param lc 160
     * @param le 60
     * @param ls 60
     */

    public CGSecurityParameters() {
        this(1024, 1024, 450, 230, 160, 30, 30, "SHA-1");
    }

    public CGSecurityParameters(int keysize) {
        this(2048, 2048, 504, 282, 160, 60, 60, "SHA-1");
    }

    public CGSecurityParameters(int ln, int lP, int lE, int lQ, int lc, int le, int ls, String hash) {
        this.ln = ln;
        this.lP = lP;
        this.lE = lE;
        this.lQ = lQ;
        this.lc = lc;
        this.le = le;
        this.ls = ls;
        this.hash = hash;
    }

    public CGSecurityParameters(byte[] encoded) {
        ASN1InputStream dIn = new ASN1InputStream(new ByteArrayInputStream(encoded));

        DLSequence s = null;
        try {
            s = (DLSequence) dIn.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode Public Key object");
        }

        this.ln = ((ASN1Integer) s.getObjectAt(0)).getValue().intValue();
        this.lP = ((ASN1Integer) s.getObjectAt(1)).getValue().intValue();
        this.lE = ((ASN1Integer) s.getObjectAt(2)).getValue().intValue();
        this.lQ = ((ASN1Integer) s.getObjectAt(3)).getValue().intValue();
        this.lc = ((ASN1Integer) s.getObjectAt(4)).getValue().intValue();
        this.le = ((ASN1Integer) s.getObjectAt(5)).getValue().intValue();
        this.ls = ((ASN1Integer) s.getObjectAt(6)).getValue().intValue();
        this.hash = ((DERIA5String) s.getObjectAt(7)).getString();
    }

    public int getln() {
        return ln;
    }

    public int getlP() {
        return lP;
    }

    public int getlE() {
        return lE;
    }

    public int getlQ() {
        return lQ;
    }

    public int getlc() {
        return lc;
    }

    public int getle() {
        return le;
    }

    public int getls() {
        return ls;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(ln));
        v.add(new ASN1Integer(lP));
        v.add(new ASN1Integer(lE));
        v.add(new ASN1Integer(lQ));
        v.add(new ASN1Integer(lc));
        v.add(new ASN1Integer(le));
        v.add(new ASN1Integer(ls));
        v.add(new DERIA5String(hash));

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
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("\n\tln: ");
        s.append(ln);
        s.append("\n\tlP: ");
        s.append(lP);
        s.append("\n\tlE: ");
        s.append(lE);
        s.append("\n\tlQ: ");
        s.append(lQ);
        s.append("\n\tlc: ");
        s.append(lc);
        s.append("\n\tle: ");
        s.append(le);
        s.append("\n\tls: ");
        s.append(ls);
        s.append("\n\tHash: ");
        s.append(hash);

        return s.toString();
    }

}
