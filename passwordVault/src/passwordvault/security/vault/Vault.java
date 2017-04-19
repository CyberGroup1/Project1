/*
 * This class' responsibility is to abstract away the KeyStore from the end-user.
 *
 * Also, the vault package is so that Vault & VaultEntry can share methods
 * that other classes don't need access to. (implementation details)
 ***************************
 *  VaultEntries are stored in a doubly-linked list.
 *  To get all the entries: start from the first entry,
 *          and call VaultEntry.getNextEntryId()...
 * For the UI, use VaultListModel.
 */
package passwordvault.security.vault;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.UnrecoverableKeyException;

/**
 * This class opens a KeyStore from a file. Once opened, it lets people view the
 * usernames/passwords stored inside.
 * 
 * KeyStore can throw a lot of errors. Not sure whether I should just capture them,
 * or re-throw them.
 */
public class Vault {
    /* Note to self: Vault shouldn't hold references to VaultEntries.
     * VaultEntries already hold references to the vault.
     *     => circular-reference thing, stopping both from being garbage-collected
     */
    KeyStoreWrapper keyStore; // Can be accessed by VaultEntry
    WeakReference<VaultListener> listener = null;
    private int firstEntryId; // Ids probably used a lot, so keep a copy in memory
    private int lastEntryId;
    
    private static final String KEYFILE_ALIAS = "keyfile";
    private static final String FIRST_ENTRY_ALIAS = "first";
    private static final String LAST_ENTRY_ALIAS = "last";
    
    /**
     * Load a vault from a file. If file doesn't exist, an empty vault will be made.
     * Char[] is used to ensure that it won't be cached.
     * @param filename File to read from
     * @param password Password the file was encrypted with
     * @throws UnrecoverableKeyException 
     */
    public Vault(String filename, char password[]) throws UnrecoverableKeyException {
        keyStore = new KeyStoreWrapper(new File(filename), password);
        try {
            firstEntryId = keyStore.getIdKey(FIRST_ENTRY_ALIAS);
        } catch (InstanceNotFoundException ex) { // Set default firstEntryId
            // TODO: Find proper firstEntry? low priority
            firstEntryId = VaultEntry.MISSING_ID;
            keyStore.addIdKey(FIRST_ENTRY_ALIAS, firstEntryId);
        }
        
        try {
            lastEntryId = keyStore.getIdKey(LAST_ENTRY_ALIAS);
        } catch (InstanceNotFoundException ex) { // Set default lastEntryId
            // TODO: Find proper lastEntry? low priority
            lastEntryId = VaultEntry.MISSING_ID;
            keyStore.addIdKey(LAST_ENTRY_ALIAS, lastEntryId);
        }
    }
    
    /**
     * Get the first VaultEntry in the vault.
     * If there aren't any entries in Vault, returns null.
     * @return First vault entry in the vault.
     */
    public VaultEntry getFirstEntry() {
        return VaultEntry.getEntry(this, firstEntryId);
    }
    /**
     * Get the last VaultEntry in the vault.
     * If there aren't any entries in Vault, returns null.
     * @return Last vault entry in the vault.
     */
    public VaultEntry getLastEntry() {
        return VaultEntry.getEntry(this, lastEntryId);
    }
    /**
     * Get a VaultEntry by its id.
     * WARNING: Entries are not sorted by id numbers!
     * If it doesn't exist, returns null.
     * @param id
     * @return 
     */
    public VaultEntry getEntry(int id) {
        // I want VaultEntry to manage its aliases, so Vault calls VaultEntry
        return VaultEntry.getEntry(this, id);
    }
    
    // Might be useful for prompting to change keyFiles.
    /**
     * Get the keyFile associated with this vault.
     * If vault doesn't use a keyFile, returns null.
     * @return Key file used for the password to lock/unlock this vault
     */
    public String getKeyFile() {
        try {
            return new String(keyStore.getKey(KEYFILE_ALIAS));
        } catch (InstanceNotFoundException ex) { // If keyfile not found, it doesn't exist
            return null;
        }
    }
    
    // For use with getKeyFile. Might just be a path to the keyFile.
    /**
     * Change the vault's password to use this keyFile.
     * If keyFile is null, the keyFile will be removed.
     * @param keyFile 
     */
    public void setKeyFile(String keyFile) {
        if (keyFile != null)
            keyStore.addKey(KEYFILE_ALIAS, keyFile.toCharArray());
        // TODO: delete keyfile
    }
    
    /**
     * Save the vault to a file.
     */
    public void save() {
        keyStore.save();
    }
    
    //**************************/
    
    int getFirstEntryId() {
        return firstEntryId;
    }
    int getLastEntryId() {
        return lastEntryId;
    }
    
    void setFirstEntryId(int id) {
        firstEntryId = id;
        keyStore.addIdKey(FIRST_ENTRY_ALIAS, id);
    }
    void setLastEntryId(int id) {
        lastEntryId = id;
        keyStore.addIdKey(LAST_ENTRY_ALIAS, id);
    }
    
    //**************************/
    // Listener stuff is protected because I don't want outside classes to
    // change Vault's listener.
    
    void setListener(VaultListener l) {
        if (listener != null && listener.get() != null)
            System.err.println("WARNING: VaultListModel was overwritten.");
        if (l == null)
            listener = null;
        else
            listener = new WeakReference<>(l);
    }
    
    interface VaultListener {
        void onKeyAdded(VaultEntry entry);
        void onKeyChanged(VaultEntry entry);
        void onKeyRemoved(VaultEntry entry);
    }
}