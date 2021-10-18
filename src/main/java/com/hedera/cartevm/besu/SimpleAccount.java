package com.hedera.cartevm.besu;

import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.AccountStorageEntry;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.EvmAccount;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.ModificationNotAllowedException;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.Wei;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;

public class SimpleAccount implements EvmAccount, MutableAccount {

	private final Account parent;
	private final Map<UInt256, UInt256> storage = new HashMap<>();
	private Address address;
	private final Supplier<Hash> addressHash =
			Suppliers.memoize(() -> address == null ? Hash.ZERO : Hash.hash(address));
	private long nonce;
	private Wei balance;
	private Bytes code;
	private Supplier<Hash> codeHash =
			Suppliers.memoize(() -> code == null ? Hash.EMPTY : Hash.hash(code));

	public SimpleAccount(final Address address, final long nonce, final Wei balance) {
		this(null, address, nonce, balance, Bytes.EMPTY);
	}

	public SimpleAccount(
			final Account parent,
			final Address address,
			final long nonce,
			final Wei balance,
			final Bytes code) {
		this.parent = parent;
		this.address = address;
		this.nonce = nonce;
		this.balance = balance;
		this.code = code;
	}

	@Override
	public Address getAddress() {
		return address;
	}

	@Override
	public Hash getAddressHash() {
		return addressHash.get();
	}

	@Override
	public long getNonce() {
		return nonce;
	}

	@Override
	public void setNonce(final long value) {
		nonce = value;
	}

	@Override
	public Wei getBalance() {
		return balance;
	}

	@Override
	public void setBalance(final Wei value) {
		balance = value;
	}

	@Override
	public Bytes getCode() {
		return code;
	}

	@Override
	public void setCode(final Bytes code) {
		this.code = code;
		codeHash = Suppliers.memoize(() -> this.code == null ? Hash.EMPTY : Hash.hash(this.code));
	}

	@Override
	public Hash getCodeHash() {
		return codeHash.get();
	}

	@Override
	public UInt256 getStorageValue(final UInt256 key) {
		if (storage.containsKey(key)) {
			return storage.get(key);
		} else {
			return getOriginalStorageValue(key);
		}
	}

	@Override
	public UInt256 getOriginalStorageValue(final UInt256 key) {
		if (parent != null) {
			return parent.getStorageValue(key);
		} else {
			return UInt256.ZERO;
		}
	}

	@Override
	public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
			final Bytes32 startKeyHash, final int limit) {
		throw new UnsupportedOperationException("NGMI");
	}

	@Override
	public MutableAccount getMutable() throws ModificationNotAllowedException {
		return this;
	}

	@Override
	public void setStorageValue(final UInt256 key, final UInt256 value) {
		storage.put(key, value);
	}

	@Override
	public void clearStorage() {
		storage.clear();
	}

	@Override
	public Map<UInt256, UInt256> getUpdatedStorage() {
		return storage;
	}
}
