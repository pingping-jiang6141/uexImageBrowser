/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import android.view.VelocityTracker;

/**
 * Helper for accessing newer features in VelocityTracker.
 */
public class VelocityTrackerCompat {
	/**
	 * Interface for the full API.
	 */
	interface VelocityTrackerVersionImpl {
		public float getXVelocity(VelocityTracker tracker, int pointerId);

		public float getYVelocity(VelocityTracker tracker, int pointerId);
	}

	/**
	 * Interface implementation that doesn't use anything about v4 APIs.
	 */
	static class BaseVelocityTrackerVersionImpl implements
			VelocityTrackerVersionImpl {
		@Override
		public float getXVelocity(VelocityTracker tracker, int pointerId) {
			return tracker.getXVelocity();
		}

		@Override
		public float getYVelocity(VelocityTracker tracker, int pointerId) {
			return tracker.getYVelocity();
		}
	}

	/**
	 * Select the correct implementation to use for the current platform.
	 */
	static final VelocityTrackerVersionImpl IMPL;
	static {
		IMPL = new BaseVelocityTrackerVersionImpl();
	}

	// -------------------------------------------------------------------

	/**
	 * Call {@link VelocityTracker#getXVelocity(int)}. If running on a pre-
	 * {@android.os.Build.VERSION_CODES#HONEYCOMB} device, returns
	 * {@link VelocityTracker#getXVelocity()}.
	 */
	public static float getXVelocity(VelocityTracker tracker, int pointerId) {
		return IMPL.getXVelocity(tracker, pointerId);
	}

	/**
	 * Call {@link VelocityTracker#getYVelocity(int)}. If running on a pre-
	 * {@android.os.Build.VERSION_CODES#HONEYCOMB} device, returns
	 * {@link VelocityTracker#getYVelocity()}.
	 */
	public static float getYVelocity(VelocityTracker tracker, int pointerId) {
		return IMPL.getYVelocity(tracker, pointerId);
	}
}