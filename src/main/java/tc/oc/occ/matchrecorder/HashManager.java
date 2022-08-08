package tc.oc.occ.matchrecorder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class HashManager {

  private KeyFactory keyFactory = null;
  private PrivateKey privateKey = null;
  private PublicKey publicKey = null;
  private Signature signature = null;

  public HashManager(File privateFile, File publicFile) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    this.keyFactory = KeyFactory.getInstance("RSA");
    if (!privateFile.exists() || !publicFile.exists()) {
      createKeys(privateFile, publicFile);
    } else {
      readPrivateKey(privateFile);
      readPublicKey(publicFile);
    }
    reset();
  }

  public void addSign(byte[] data) throws Exception {
    this.signature.update(data);
  }

  public byte[] sign() throws Exception {
    return this.signature.sign();
  }

  public void reset() throws Exception {
    this.signature = Signature.getInstance("SHA256withRSA");
    this.signature.initSign(this.privateKey);
  }

  private void readPrivateKey(File privateFile) throws Exception {
    FileReader keyReader = new FileReader(privateFile);
    PemReader pemReader = new PemReader(keyReader);
    PemObject pemObject = pemReader.readPemObject();
    byte[] content = pemObject.getContent();
    PKCS8EncodedKeySpec pubKeySpec = new PKCS8EncodedKeySpec(content);
    this.privateKey = this.keyFactory.generatePrivate(pubKeySpec);
    pemReader.close();
  }

  private void readPublicKey(File publicFile) throws Exception {
    FileReader keyReader = new FileReader(publicFile);
    PemReader pemReader = new PemReader(keyReader);
    PemObject pemObject = pemReader.readPemObject();
    byte[] content = pemObject.getContent();
    X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
    this.publicKey = this.keyFactory.generatePublic(pubKeySpec);
    pemReader.close();
  }

  private void createKeys(File privateFile, File publicFile) throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(1024);
    KeyPair pair = keyPairGenerator.genKeyPair();
    this.privateKey = pair.getPrivate();
    this.publicKey = pair.getPublic();
    JcaPEMWriter privateWriter = new JcaPEMWriter(new FileWriter(privateFile));
    privateWriter.writeObject(new JcaPKCS8Generator(this.privateKey, null));
    privateWriter.close();
    JcaPEMWriter publicWriter = new JcaPEMWriter(new FileWriter(publicFile));
    publicWriter.writeObject(this.publicKey);
    publicWriter.close();
  }
}
