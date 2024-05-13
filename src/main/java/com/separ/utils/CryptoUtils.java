package com.separ.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;

import com.separ.entity.EntityType;

import it.unibs.gs.GSGroupManager;
import it.unibs.gs.GSSignature;
import it.unibs.gs.certs.GSCertificateInterface;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.cg.CGProvider;
import it.unibs.gs.cg.CGSecurityParameters;
import it.unibs.gs.interfaces.GSPublicKey;

public class CryptoUtils {

    static {
        Security.addProvider(new CGProvider());
    }

    private Map<EntityType, GSPublicKey> allGPKs;
    private GSPublicKey myGPK;
    private GSMemberCertificate memberCertificate;
    Printer printer;

    private static PublicKey raPublicKey;

    public CryptoUtils(Map<EntityType, GSPublicKey> allGPKs, GSPublicKey myGPK, GSMemberCertificate memberCertificate,
            PublicKey raPublicKey) {

        this.printer = new Printer(null);
        this.allGPKs = allGPKs;
        this.myGPK = myGPK;
        this.memberCertificate = memberCertificate;
        CryptoUtils.raPublicKey = raPublicKey;
    }

    public byte[] groupSign(byte[] data) {
        var engine = getEngine();
        var params = new CGSecurityParameters();

        try {
            engine.initSign(myGPK, memberCertificate.getMemberKey(), params, new SecureRandom());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            engine.update(data);
            return engine.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean groupVerify(EntityType group, byte[] data, byte[] signature) {
        printer.info("reached groupVerify");

        var engine = getEngine();
        var params = new CGSecurityParameters();

        var gpk = allGPKs.get(group);

        try {
            engine.initVerify(gpk, params);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            engine.update(data);
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        try {
            // System.out.println(new CGSignatureImpl(signature));
            return engine.verify(signature);
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static byte[] encodeCert(GSCertificateInterface certificate) {
        var bytestream = new ByteArrayOutputStream();
        try {
            var objstream = new ObjectOutputStream(bytestream);
            objstream.writeObject(certificate);
            return bytestream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static GSCertificateInterface decodeCert(byte[] data) {
        var bytestream = new ByteArrayInputStream(data);
        try {
            var objstream = new ObjectInputStream(bytestream);
            var certificate = (GSCertificateInterface) objstream.readObject();
            return certificate;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] encodeKey(Key key) {
        var bytestream = new ByteArrayOutputStream();
        try {
            var objstream = new ObjectOutputStream(bytestream);
            objstream.writeObject(key);
            return bytestream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Key decodeKey(byte[] data) {
        var bytestream = new ByteArrayInputStream(data);
        try {
            var objstream = new ObjectInputStream(bytestream);
            var key = (Key) objstream.readObject();
            return key;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static GSGroupManager getManager() {
        try {
            return GSGroupManager.getInstance("CG");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static GSSignature getEngine() {
        try {
            return GSSignature.getInstance("CG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] sign(byte[] message, PrivateKey privateKey) {
        try {
            Signature signEngine = Signature.getInstance("SHA1withRSA");
            signEngine.initSign(privateKey);
            signEngine.update(message);
            byte[] signature = signEngine.sign();

            return signature;
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean verify(byte[] message, byte[] signature, PublicKey publicKey) {
        if (signature == null)
            return false;

        try {
            Signature signEngine = Signature.getInstance("SHA1withRSA");
            signEngine.initVerify(publicKey);
            signEngine.update(message);
            return signEngine.verify(signature);
        } catch (NoSuchAlgorithmException nsae) {
            return false;
        } catch (SignatureException se) {
            return false;
        } catch (InvalidKeyException ike) {
            return false;
        }
    }
}
