package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.Serializable;

public class ImageItem implements Serializable {
	/**
	 * @Fields serialVersionUID : TODO
	 */
	private static final long serialVersionUID = -8015462855272325673L;
	public static final int TAG_NULL_IMAGE = 0;
	public static final int TAG_TINY_IMAGE = 1;
	public static final int TAG_SRC_IMAGE = 2;
	private MultiTouchImageView imageView;
	private ImageInfo imageInfo;

	public ImageItem() {
		imageInfo = new ImageInfo();
	}

	public MultiTouchImageView getImageView() {
		return imageView;
	}

	public void setImageView(MultiTouchImageView imageView) {
		this.imageView = imageView;
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	public void setImageInfo(ImageInfo imageInfo) {
		this.imageInfo = imageInfo;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof ImageItem) {
			final ImageItem imageItem = (ImageItem) o;
			if (imageInfo.srcUrl.equals(imageItem.getImageInfo().srcUrl)) {
				return true;
			}
		}
		return super.equals(o);
	}

}