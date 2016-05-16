/*
 *
 */
package eu.gaki.ffp.domain;

// TODO: Auto-generated Javadoc
/**
 * The Enum StatusEnum.
 */
public enum StatusEnum {

    /** The watch. */
    WATCH(10),

    /** The to send. */
    TO_SEND(20),

    /** The sending. */
    SENDING(30),

    /** The sended. */
    SENDED(40),

    /** The archived. */
    ARCHIVED(50);

    /**
     * Order in the lifecycle
     */
    private int order = 0;

    /**
     * Constructor.
     *
     * @param order
     *            the order in the lifecycle.
     */
    private StatusEnum(final int order) {
	this.order = order;
    }

}
