# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# this file is included from Android.mk files to build a target-specific
# shared library
#

LOCAL_BUILD_SCRIPT := BUILD_SHARED_LIBRARY
LOCAL_MODULE_CLASS := SHARED_LIBRARY
LOCAL_MAKEFILE     := $(local-makefile)

$(call check-defined-LOCAL_MODULE,$(LOCAL_BUILD_SCRIPT))
$(call check-LOCAL_MODULE,$(LOCAL_MAKEFILE))

# we are building target objects
my := TARGET_

LOCAL_BUILT_MODULE := $(call shared-library-path,$(LOCAL_MODULE))
LOCAL_OBJS_DIR     := $(TARGET_OBJS)/$(LOCAL_MODULE)

$(call module-add-shared-library,$(LOCAL_MODULE),$(LOCAL_BUILT_MODULE),$(LOCAL_MAKEFILE))

include $(BUILD_SYSTEM)/build-binary.mk

$(LOCAL_BUILT_MODULE): $(LOCAL_OBJECTS)
	@ mkdir -p $(dir $@)
	@ echo "SharedLibrary  : $(PRIVATE_NAME)"
	$(hide) $(cmd-build-shared-library)

ALL_SHARED_LIBRARIES += $(LOCAL_BUILT_MODULE)

include $(BUILD_SYSTEM)/install-binary.mk
