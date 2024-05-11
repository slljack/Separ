package it.unibs.gs.certs;

import java.math.BigInteger;
import java.util.Date;

import it.unibs.gs.interfaces.GSPublicKey;
import it.unibs.gs.interfaces.GSSecurityParameters;

public interface GSCertificateInterface {

    byte[] getEncoded();

    byte[] getEncodedForSigning();

    String toString();

    void addSignature(byte[] signature);

    Date getStartDate();

    Date getExpiryDate();

    Date getTimestamp();

    BigInteger getSerialNumber();

    String getIssuer();

    String getSubject();

    String getAlgorithm();

    GSPublicKey getGPK();

    float getVersion();

    GSSecurityParameters getParams();

    byte[] getSignature();

}