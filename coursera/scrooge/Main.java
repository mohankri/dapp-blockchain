
import java.security.*;
import java.io.*;
import java.math.BigInteger;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        System.out.println("Hello World");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv_km = pair.getPrivate();
        PublicKey pub_km = pair.getPublic();

        pair = keyGen.generateKeyPair();
        PrivateKey priv_another = pair.getPrivate();
        PublicKey pub_another = pair.getPublic();

        Transaction tx = new Transaction();
        tx.addOutput(10, pub_km);

        byte[] initHash = BigInteger.valueOf(1695609641).toByteArray();
        tx.addInput(initHash, 0);


        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(priv_km);
        signature.update(tx.getRawDataToSign(0));
        byte[] sig = signature.sign();

        tx.addSignature(sig, 0);
        tx.finalize();

        /* root unspent transaction is add to UTXO Pool */
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), 0);

        utxoPool.addUTXO(utxo, tx.getOutput(0));
        

        Transaction tx2 = new Transaction();

        tx2.addInput(tx.getHash(), 0);

        tx2.addOutput(5, pub_another);
        tx2.addOutput(3, pub_another);
        tx2.addOutput(2, pub_another);

        signature.initSign(priv_km);
        signature.update(tx2.getRawDataToSign(0));
        sig = signature.sign();

        tx2.addSignature(sig, 0);
        tx2.finalize();

        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println("Is Valid " + txHandler.isValidTx(tx2));
        System.out.println("Handle Txs " + txHandler.handleTxs(new Transaction[]{tx2}).length);
        return;
    }
}