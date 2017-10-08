import java.security.*;
import java.util.concurrent.*;
import java.util.List;

//import com.sun.corba.se.spi.orbutil.fsm.Input;

import java.util.ArrayList;

public class TxHandler {
    private UTXOPool myTranscationPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        myTranscationPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> checkList = new ArrayList<UTXO>();
        /* Get all the input in the transaction */
        double inputSum = 0;
        double outputSum = 0;
        int index = 0;
        for (Transaction.Input in: tx.getInputs()) {
            UTXO checkUtxo = new UTXO(in.prevTxHash, in.outputIndex);
            /* double spent */
            if (checkList.contains(checkUtxo)) return false;

            checkList.add(checkUtxo);

            /* re-verify */
            if (!myTranscationPool.contains(checkUtxo)) return false;

            inputSum += myTranscationPool.getTxOutput(checkUtxo).value;

            Transaction.Output TxOut = myTranscationPool.getTxOutput(checkUtxo);
            PublicKey pubKey = TxOut.address;
            byte[] message = tx.getRawDataToSign(index);
            byte[] signature = in.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) {
                    return false;
            }
            index++;
        }
        for (Transaction.Output out: tx.getOutputs()) {
            if (out.value < 0) return false;
            outputSum += out.value;
        }

        if (outputSum > inputSum) return false;
        
       return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        if (possibleTxs == null) return new Transaction[0];
        List<Transaction> validTransaction = new ArrayList<>();
        for (Transaction entry: possibleTxs) {
            if (isValidTx(entry)) {
                validTransaction.add(entry);
                for (Transaction.Input inputEntry: entry.getInputs()) {
                    UTXO spent = new UTXO(inputEntry.prevTxHash, inputEntry.outputIndex);
                    myTranscationPool.removeUTXO(spent);
                }
                int index = 0;
                for (Transaction.Output outEntry: entry.getOutputs()) {
                    UTXO validTx = new UTXO(entry.getHash(), index);
                    index++;
                    myTranscationPool.addUTXO(validTx, outEntry);
                }
            }
        }

        return validTransaction.toArray(new Transaction[validTransaction.size()]);
    }
}
