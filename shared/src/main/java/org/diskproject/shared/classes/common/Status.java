package org.diskproject.shared.classes.common;

public enum Status {
    QUEUED, RUNNING, FAILED, SUCCESSFUL, PENDING, INTERNAL_ERROR;

    public static Status fromString (String str) {
		try {
			return Status.valueOf(str);
		} catch (Exception e) {
			if (str != null && str.equals("SUCCESS"))
				return Status.SUCCESSFUL;
			return Status.INTERNAL_ERROR;
		}
    }
};