package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.spec.state.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Builder
@AllArgsConstructor
public class DisplayMessageState extends State<Long> {
    private String messageLine1;
    private String messageLine2;
    private Number messageBeeps;
    private String messageType;
    // true = ascii, false = unicode
    private boolean ascii;

    public DisplayMessageState() {
    }

    public DisplayMessageState(Long state) {
        super(state);
    }

    public String getMessageLine1() {
        return messageLine1;
    }

    public void setMessageLine1(String messageLine1) {
        this.messageLine1 = messageLine1;
    }

    public String getMessageLine2() {
        return messageLine2;
    }

    public void setMessageLine2(String messageLine2) {
        this.messageLine2 = messageLine2;
    }

    public Number getMessageBeeps() {
        return messageBeeps;
    }

    public void setMessageBeeps(Number messageBeeps) {
        this.messageBeeps = messageBeeps;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isMessage() {
        return this.getMessageType() != null && !"alarm".equalsIgnoreCase(this.getMessageType());
    }

    public boolean isAscii() {
        return ascii;
    }

    public void setAscii(boolean ascii) {
        this.ascii = ascii;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DisplayMessageState that = (DisplayMessageState) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(messageLine1, that.messageLine1).append(messageLine2, that.messageLine2).append(messageBeeps, that.messageBeeps).append(messageType, that.messageType).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(messageLine1).append(messageLine2).append(messageBeeps).append(messageType).toHashCode();
    }
}
