package org.qora.data.transaction;

import java.math.BigDecimal;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.qora.transaction.Transaction.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;

// All properties to be converted to JSON via JAX-RS
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(allOf = { TransactionData.class })
public class RemoveGroupAdminTransactionData extends TransactionData {

	// Properties
	@Schema(description = "group owner's public key", example = "2tiMr5LTpaWCgbRvkPK8TFd7k63DyHJMMFFsz9uBf1ZP")
	private byte[] ownerPublicKey;
	@Schema(description = "group ID")
	private int groupId;
	@Schema(description = "admin to demote", example = "QixPbJUwsaHsVEofJdozU9zgVqkK6aYhrK")
	private String admin; 
	/** Reference to transaction that triggered adminship. */
	// For internal use when orphaning
	@XmlTransient
	@Schema(hidden = true)
	private byte[] adminReference;

	// Constructors

	// For JAX-RS
	protected RemoveGroupAdminTransactionData() {
		super(TransactionType.REMOVE_GROUP_ADMIN);
	}

	public void afterUnmarshal(Unmarshaller u, Object parent) {
		this.creatorPublicKey = this.ownerPublicKey;
	}

	public RemoveGroupAdminTransactionData(byte[] ownerPublicKey, int groupId, String admin, byte[] adminReference, BigDecimal fee, long timestamp, byte[] reference, byte[] signature) {
		super(TransactionType.REMOVE_GROUP_ADMIN, fee, ownerPublicKey, timestamp, reference, signature);

		this.ownerPublicKey = ownerPublicKey;
		this.groupId = groupId;
		this.admin = admin;
		this.adminReference = adminReference;
	}

	/** Constructor typically used after deserialization */
	public RemoveGroupAdminTransactionData(byte[] ownerPublicKey, int groupId, String admin, BigDecimal fee, long timestamp, byte[] reference, byte[] signature) {
		this(ownerPublicKey, groupId, admin, null, fee, timestamp, reference, signature);
	}

	// Getters / setters

	public byte[] getOwnerPublicKey() {
		return this.ownerPublicKey;
	}

	public int getGroupId() {
		return this.groupId;
	}

	public String getAdmin() {
		return this.admin;
	}

	public byte[] getAdminReference() {
		return this.adminReference;
	}

	public void setAdminReference(byte[] adminReference) {
		this.adminReference = adminReference;
	}

}
