package edu.umkc.csee5110;

import java.io.File;

public class FileTransferModel {

	private File file;
	private String otherUser;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getOtherUser() {
		return otherUser;
	}

	public void setOtherUser(String otherUser) {
		this.otherUser = otherUser;
	}

	public FileTransferModel(File file, String otherUser) {
		super();
		this.file = file;
		this.otherUser = otherUser;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((otherUser == null) ? 0 : otherUser.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FileTransferModel)) {
			return false;
		}
		FileTransferModel other = (FileTransferModel) obj;
		if (file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!file.equals(other.file)) {
			return false;
		}
		if (otherUser == null) {
			if (other.otherUser != null) {
				return false;
			}
		} else if (!otherUser.equals(other.otherUser)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "FileTransferModel [file=" + file + ", otherUser=" + otherUser + "]";
	}

}
