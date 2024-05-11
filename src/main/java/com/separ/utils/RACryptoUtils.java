package com.separ.utils;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.separ.entity.EntityType;

import it.unibs.gs.certs.GSGroupCertificate;
import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.cg.CGGroupManager;
import it.unibs.gs.cg.CGSecurityParameters;
import it.unibs.gs.interfaces.GSKeyPair;

public class RACryptoUtils {

    private static KeyPair signatureKeyPair = generateKeyPair();
    private static Map<EntityType, GSKeyPair> groupKeyPairs = new HashMap<EntityType, GSKeyPair>();
    private static Map<EntityType, GSGroupCertificate> groupCertificates = new HashMap<EntityType, GSGroupCertificate>();
    private static Map<EntityType, Map<Integer, GSMemberCertificate>> memberCertificates = new HashMap<EntityType, Map<Integer, GSMemberCertificate>>();
    private static CGGroupManager manager = new CGGroupManager();

    public static void generateGroupCertificate(EntityType group) {
        var params = new CGSecurityParameters();
        var keyPair = generateGroupKeyPair(group);

        Calendar calendar = Calendar.getInstance();
        Date startDate = calendar.getTime();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.YEAR, 1);
        Date expiryDate = calendar2.getTime();
        BigInteger serialNumber = BigInteger.ONE;
        var groupCert = manager.engineBuildGroupCertificate(keyPair.getPublic(), "RA", group.id(), startDate,
                expiryDate, serialNumber, params);

        groupCert.addSignature(CryptoUtils.sign(groupCert.getEncodedForSigning(), signatureKeyPair.getPrivate()));

        assert CryptoUtils.verify(groupCert.getEncodedForSigning(), groupCert.getSignature(),
                signatureKeyPair.getPublic()) == true;

        groupCertificates.put(group, groupCert);
        memberCertificates.put(group, new HashMap<Integer, GSMemberCertificate>());
    }

    public static void generateMemberCertificate(int id, EntityType group) {

        if (!groupCertificates.containsKey(group)) {
            generateGroupCertificate(group);
        }

        var groupCert = groupCertificates.get(group);
        var keyPair = groupKeyPairs.get(group);

        var memberCert = manager.engineDoJoin(group.id() + "-" + id, groupCert, keyPair.getPrivate());
        memberCertificates.get(group).put(id, memberCert);
    }

    private static GSKeyPair generateGroupKeyPair(EntityType group) {
        var params = new CGSecurityParameters();

        try {
            var keyPair = manager.engineDoSetup(params);
            groupKeyPairs.put(group, keyPair);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static PublicKey getSignaturePublicKey() {
        return signatureKeyPair.getPublic();
    }

    public static PrivateKey getSignaturePrivateKey() {
        return signatureKeyPair.getPrivate();
    }

    public static GSGroupCertificate getGroupCertificate(EntityType group) {
        return groupCertificates.get(group);
    }

    public static Map<EntityType, Map<Integer, GSMemberCertificate>> getMemberCertificates() {
        return memberCertificates;
    }
}
