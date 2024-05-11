package com.separ.data.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import com.separ.data.MessageInterface;
import com.separ.entity.EntityType;
import com.separ.utils.CryptoUtils;

import it.unibs.gs.certs.GSMemberCertificate;
import it.unibs.gs.cg.CGSecurityParameters;
import it.unibs.gs.interfaces.GSPublicKey;

public class CryptoMessage implements MessageInterface {

    private Map<EntityType, GSPublicKey> allGPKs;
    private GSPublicKey myGPK;
    private GSMemberCertificate memberCertificate;

    private PublicKey raPublicKey;

    public CryptoMessage() {

    }

    public CryptoMessage(Map<EntityType, GSPublicKey> allGPKs, GSPublicKey myGPK, GSMemberCertificate memberCertificate,
            PublicKey raPublicKey) {
        this.allGPKs = allGPKs;
        this.myGPK = myGPK;
        this.memberCertificate = memberCertificate;
        this.raPublicKey = raPublicKey;
    }

    public Map<EntityType, GSPublicKey> getAllGPKs() {
        return allGPKs;
    }

    public void setAllGPKs(Map<EntityType, GSPublicKey> allGPKs) {
        this.allGPKs = allGPKs;
    }

    public GSPublicKey getMyGPK() {
        return myGPK;
    }

    public void setMyGPK(GSPublicKey myGPK) {
        this.myGPK = myGPK;
    }

    public GSMemberCertificate getMemberCertificate() {
        return memberCertificate;
    }

    public void setMemberCertificate(GSMemberCertificate memberCertificate) {
        this.memberCertificate = memberCertificate;
    }

    public PublicKey getRaPublicKey() {
        return raPublicKey;
    }

    public void setRaPublicKey(PublicKey raPublicKey) {
        this.raPublicKey = raPublicKey;
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(allGPKs.size());
            for (var group : allGPKs.keySet()) {
                outstream.writeInt(group.ordinal());
                var data = CryptoUtils.encodeKey(allGPKs.get(group));
                outstream.writeInt(data.length);
                outstream.write(data);
            }

            var data2 = CryptoUtils.encodeKey(myGPK);
            outstream.writeInt(data2.length);
            outstream.write(data2);

            var params = memberCertificate.params;
            memberCertificate.params = null;
            var data3 = CryptoUtils.encodeCert(memberCertificate);
            memberCertificate.params = params;
            outstream.writeInt(data3.length);
            outstream.write(data3);

            var data4 = CryptoUtils.encodeKey(raPublicKey);
            outstream.writeInt(data4.length);
            outstream.write(data4);

            return bytestream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void fromByteArray(byte[] data) {

        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            var gpkCount = instream.readInt();
            allGPKs = new HashMap<EntityType, GSPublicKey>();
            for (var i = 0; i < gpkCount; i++) {
                var group = EntityType.values()[instream.readInt()];
                var len = instream.readInt();
                var keydata = new byte[len];
                instream.read(keydata);
                var gpk = (GSPublicKey) CryptoUtils.decodeKey(keydata);
                allGPKs.put(group, gpk);
            }

            var len2 = instream.readInt();
            var keydata2 = new byte[len2];
            instream.read(keydata2);
            myGPK = (GSPublicKey) CryptoUtils.decodeKey(keydata2);

            var len3 = instream.readInt();
            var certdata = new byte[len3];
            instream.read(certdata);
            memberCertificate = (GSMemberCertificate) CryptoUtils.decodeCert(certdata);
            memberCertificate.params = new CGSecurityParameters();

            var len4 = instream.readInt();
            var keydata4 = new byte[len4];
            instream.read(keydata4);
            raPublicKey = (PublicKey) CryptoUtils.decodeKey(keydata4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "CryptoMessage [allGPKs=" + allGPKs + ", myGPK=" + myGPK + ", memberCertificate=" + memberCertificate
                + ", raPublicKey=" + raPublicKey + "]";
    }
}
