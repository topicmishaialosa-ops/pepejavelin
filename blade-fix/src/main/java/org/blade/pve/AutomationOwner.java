package org.blade.pve;

public interface AutomationOwner {
    default String automationId() {
        return getClass().getSimpleName();
    }

    default void onAutomationRevoked(PveAutomationCoordinator.RevocationReason reason) {
    }
}
