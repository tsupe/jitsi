/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.msnconstants.*;
import net.sf.jml.MsnContact;

/**
 * The Msn implementation of the service.protocol.Contact interface.
 * @author Damian Minkov
 */
public class ContactMsnImpl
    implements Contact
{
    private MsnContact contact = null;
    private boolean isLocal = false;
    private byte[] image = null;
    private PresenceStatus status = MsnStatusEnum.OFFLINE;
    private ServerStoredContactListMsnImpl ssclCallback = null;
    private boolean isPersistent = false;
    private boolean isResolved = false;

    /**
     * Creates an MsnContactImpl
     * @param rosterEntry the RosterEntry object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    ContactMsnImpl(MsnContact contact,
                   ServerStoredContactListMsnImpl ssclCallback,
                   boolean isPersistent,
                   boolean isResolved)
    {
        this.contact = contact;
        this.isLocal = isLocal;
        this.ssclCallback = ssclCallback;
        this.isPersistent = isPersistent;
        this.isResolved = isResolved;
    }

    /**
     * Returns the Msn Userid of this contact
     * @return the Msn Userid of this contact
     */
    public String getAddress()
    {
        if(isResolved)
            return contact.getEmail().getEmailAddress();
        else
            return contact.getId();
    }

    /**
     * Determines whether or not this Contact instance represents the user used
     * by this protocol provider to connect to the service.
     *
     * @return true if this Contact represents us (the local user) and false
     * otherwise.
     */
    public boolean isLocal()
    {
        return isLocal;
    }

    public byte[] getImage()
    {
        return image;
    }

    /**
     * Returns a hashCode for this contact. The returned hashcode is actually
     * that of the Contact's Address
     * @return the hashcode of this Contact
     */
    public int hashCode()
    {
        return getAddress().hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     *
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    public boolean equals(Object obj)
    {
        if (obj == null
            || !(obj instanceof ContactMsnImpl)
            || !(((ContactMsnImpl)obj).getAddress().equals(getAddress())
                && ((ContactMsnImpl)obj).getProtocolProvider()
                        == getProtocolProvider()))
            return false;
        else
            return true;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("MsnContact[ id=");
        buff.append(getAddress()).append("]");

        return buff.toString();
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the server.
     *
     * @param status the MsnStatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        this.status = status;
    }

    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <tt>queryContactStatus()</tt> method in
     * <tt>OperationSetPresence</tt>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return status;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    public String getDisplayName()
    {
        String name = contact.getDisplayName();

        if (name == null)
            name = getAddress();

        return name;
    }

    /**
     * Returns a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not suppord persistent
     * presence.
     * @return a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not suppord persistent
     * presence.
     */
    public ContactGroup getParentContactGroup()
    {
        return ssclCallback.findContactGroup(this);
    }


    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return ssclCallback.getParentProvider();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether this contact is to be considered persistent or not. The
     * method is to be used _only_ when a non-persistent contact has been added
     * to the contact list and its encapsulated VolatileBuddy has been repalced
     * with a standard buddy.
     * @param persistent true if the buddy is to be considered persistent and
     * false for volatile.
     */
    void setPersistent(boolean persistent)
    {
        this.isPersistent = persistent;
    }

    /**
     * Resolve this contact against the given entry
     * @param entry the server stored entry
     */
    void setResolved(MsnContact entry)
    {
        if(isResolved)
            return;

        this.isResolved = true;
        contact = entry;
    }

    /**
     * Returns the persistent data
     * @return the persistent data
     */
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    public void setPersistentData(String persistentData)
    {
    }

    /**
     * Get source contact
     * @return MsnContact
     */
    MsnContact getSourceContact()
    {
        return contact;
    }
}
