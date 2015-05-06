package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageInfo implements Parcelable {
	// 图片原始路径
	public String srcUrl;
	// 图片保存的本地路径
	public String savePath;

	public ImageInfo() {

	}

	public ImageInfo(String srcUrl, String savePath) {
		this.srcUrl = srcUrl;
		this.savePath = savePath;
	}

	public ImageInfo(Parcel in) {
		this.srcUrl = in.readString();
		this.savePath = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.srcUrl);
		dest.writeString(this.savePath);
	}

	public static final Creator<ImageInfo> CREATOR = new Creator<ImageInfo>() {

		public ImageInfo createFromParcel(Parcel in) {
			return new ImageInfo(in);
		}

		public ImageInfo[] newArray(int size) {
			return new ImageInfo[size];
		}

	};

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof ImageInfo) {
			final ImageInfo other = (ImageInfo) o;
			if (other.srcUrl.equals(this.srcUrl)) {
				return true;
			}
			return super.equals(o);
		} else {
			return false;
		}

	};
}