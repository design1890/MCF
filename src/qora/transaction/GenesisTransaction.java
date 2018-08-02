package qora.transaction;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.primitives.Bytes;

import data.transaction.GenesisTransactionData;
import data.transaction.TransactionData;
import qora.account.Account;
import qora.account.PrivateKeyAccount;
import qora.assets.Asset;
import qora.crypto.Crypto;
import repository.DataException;
import repository.Repository;
import transform.TransformationException;
import transform.transaction.TransactionTransformer;

public class GenesisTransaction extends Transaction {

	// Properties
	private GenesisTransactionData genesisTransactionData;

	// Constructors

	public GenesisTransaction(Repository repository, TransactionData transactionData) {
		super(repository, transactionData);

		if (this.transactionData.getSignature() == null)
			this.transactionData.setSignature(this.calcSignature());

		this.genesisTransactionData = (GenesisTransactionData) this.transactionData;
	}

	// More information

	public List<Account> getRecipientAccounts() throws DataException {
		return Collections.singletonList(new Account(this.repository, genesisTransactionData.getRecipient()));
	}

	public boolean isInvolved(Account account) throws DataException {
		String address = account.getAddress();

		if (address.equals(this.getCreator().getAddress()))
			return true;

		if (address.equals(genesisTransactionData.getRecipient()))
			return true;

		return false;
	}

	public BigDecimal getAmount(Account account) throws DataException {
		String address = account.getAddress();
		BigDecimal amount = BigDecimal.ZERO.setScale(8);

		// NOTE: genesis transactions have no fee, so no need to test against creator as sender

		if (address.equals(genesisTransactionData.getRecipient()))
			amount = amount.add(genesisTransactionData.getAmount());

		return amount;
	}

	// Processing

	/**
	 * Refuse to calculate genesis transaction signature!
	 * <p>
	 * This is not possible as there is no private key for the genesis account and so no way to sign data.
	 * <p>
	 * <b>Always throws IllegalStateException.</b>
	 * 
	 * @throws IllegalStateException
	 */
	@Override
	public void sign(PrivateKeyAccount signer) {
		throw new IllegalStateException("There is no private key for genesis transactions");
	}

	/**
	 * Generate genesis transaction signature.
	 * <p>
	 * This is handled differently as there is no private key for the genesis account and so no way to sign data.
	 * <p>
	 * Instead we return the SHA-256 digest of the transaction, duplicated so that the returned byte[] is the same length as normal transaction signatures.
	 * 
	 * @return byte[]
	 */
	private byte[] calcSignature() {
		try {
			byte[] digest = Crypto.digest(TransactionTransformer.toBytes(this.transactionData));
			return Bytes.concat(digest, digest);
		} catch (TransformationException e) {
			return null;
		}
	}

	/**
	 * Check validity of genesis transaction signature.
	 * <p>
	 * This is handled differently as there is no private key for the genesis account and so no way to sign/verify data.
	 * <p>
	 * Instead we compared our signature with one generated by {@link GenesisTransaction#calcSignature()}.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean isSignatureValid() {
		return Arrays.equals(this.transactionData.getSignature(), this.calcSignature());
	}

	@Override
	public ValidationResult isValid() {
		// Check amount is zero or positive
		if (genesisTransactionData.getAmount().compareTo(BigDecimal.ZERO) >= 0)
			return ValidationResult.NEGATIVE_AMOUNT;

		// Check recipient address is valid
		if (!Crypto.isValidAddress(genesisTransactionData.getRecipient()))
			return ValidationResult.INVALID_ADDRESS;

		return ValidationResult.OK;
	}

	@Override
	public void process() throws DataException {
		// Save this transaction itself
		this.repository.getTransactionRepository().save(this.transactionData);

		// Update recipient's balance
		Account recipient = new Account(repository, genesisTransactionData.getRecipient());
		recipient.setConfirmedBalance(Asset.QORA, genesisTransactionData.getAmount());

		// Set recipient's starting reference
		recipient.setLastReference(genesisTransactionData.getSignature());
	}

	@Override
	public void orphan() throws DataException {
		// Delete this transaction
		this.repository.getTransactionRepository().delete(this.transactionData);

		// Delete recipient's balance
		Account recipient = new Account(repository, genesisTransactionData.getRecipient());
		recipient.deleteBalance(Asset.QORA);

		// Delete recipient's last reference
		recipient.setLastReference(null);
	}

}
