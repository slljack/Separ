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

package it.unibs.gs.certs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNumericString;
import org.bouncycastle.asn1.DLOutputStream;
import org.bouncycastle.asn1.DLSequence;

import it.unibs.gs.GSMathCore;
import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

public abstract class GSGroupCertificate implements Serializable, GSCertificateInterface {
    /**
     *
     */
    private static final long serialVersionUID = -5591289184157149140L;

    private Date startDate, expiryDate, timestamp;
    private BigInteger serialNumber;
    private String issuer, subject;
    private String algorithm;
    protected GSPublicKey gpk;
    private float version;
    private GSSecurityParameters params;

    private byte[] signature;

    protected GSGroupCertificate(String algorithm, float version, BigInteger serialNumber, String issuer,
            String subject, Date startDate, Date expiryDate, GSPublicKey gpk, GSSecurityParameters params) {
        this.algorithm = algorithm;
        this.version = version;
        this.serialNumber = serialNumber;
        this.issuer = issuer;
        this.subject = subject;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.gpk = gpk;
        this.params = params;
        this.timestamp = new Date(System.currentTimeMillis());
        this.signature = null;
    }

    @Override
    public byte[] getEncoded() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERIA5String(algorithm));
        v.add(new DERNumericString("" + version));
        v.add(new ASN1Integer(serialNumber));
        v.add(new DERIA5String(issuer));
        v.add(new DERIA5String(subject));
        v.add(new DERGeneralizedTime(startDate));
        v.add(new DERGeneralizedTime(expiryDate));
        v.add(new DERBitString(signature));
        v.add(new DERBitString(gpk.getEncoded()));
        v.add(new DERBitString(params.getEncoded()));
        v.add(new DERGeneralizedTime(timestamp));

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
    public byte[] getEncodedForSigning() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERIA5String(algorithm));
        v.add(new DERNumericString(String.valueOf(version)));
        v.add(new ASN1Integer(serialNumber));
        v.add(new DERIA5String(issuer));
        v.add(new DERIA5String(subject));
        v.add(new DERGeneralizedTime(startDate));
        v.add(new DERGeneralizedTime(expiryDate));
        v.add(new DERGeneralizedTime(timestamp));
        v.add(new DERBitString(gpk.getEncoded()));
        v.add(new DERBitString(params.getEncoded()));

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

        s.append("Group Certificate: ");
        s.append("\n\tAlgorithm: \n\t\t");
        s.append(algorithm);
        s.append("\n\tVersion: \n\t\t");
        s.append(version);
        s.append("\n\tSerial Number: \n\t\t");
        s.append(serialNumber);
        s.append("\n\tIssuer: \n\t\t");
        s.append(issuer);
        s.append("\n\tValid after: \n\t\t");
        s.append(startDate);
        s.append("\n\tValid until: \n\t\t");
        s.append(expiryDate);
        s.append("\n\tSubject: \n\t\t");
        s.append(subject);
        s.append("\n\tTimestamp: \n\t\t");
        s.append(timestamp);
        s.append("\n\tGS Group Public Key: \n\t\t");
        s.append(GSMathCore.byteArrayToString(gpk.getEncoded()));
        s.append("\n\tGS Group Parameters: ");
        s.append(params.toString());
        s.append("\n\tDigital Signature: \n\t\t");
        s.append(GSMathCore.byteArrayToString(signature));

        return s.toString();
    }

    @Override
    public void addSignature(byte[] signature) {
        this.signature = signature;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getExpiryDate() {
        return expiryDate;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public abstract GSPublicKey getGPK();

    @Override
    public float getVersion() {
        return version;
    }

    @Override
    public GSSecurityParameters getParams() {
        return params;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }
}
