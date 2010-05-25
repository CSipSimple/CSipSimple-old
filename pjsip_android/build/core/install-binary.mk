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

# Installed module handling
#
LOCAL_INSTALLED_MODULE := $(NDK_APP_DST_DIR)/$(notdir $(LOCAL_BUILT_MODULE))

$(LOCAL_INSTALLED_MODULE): PRIVATE_NAME    := $(notdir $(LOCAL_BUILT_MODULE))
$(LOCAL_INSTALLED_MODULE): PRIVATE_SRC     := $(LOCAL_BUILT_MODULE)
$(LOCAL_INSTALLED_MODULE): PRIVATE_DST_DIR := $(NDK_APP_DST_DIR)
$(LOCAL_INSTALLED_MODULE): PRIVATE_DST     := $(LOCAL_INSTALLED_MODULE)

$(LOCAL_INSTALLED_MODULE): $(LOCAL_BUILT_MODULE) clean-installed-binaries
	@ echo "Install        : $(PRIVATE_NAME) => $(PRIVATE_DST_DIR)"
	$(hide) mkdir -p $(PRIVATE_DST_DIR)
	$(hide) install -p $(PRIVATE_SRC) $(PRIVATE_DST)
	$(hide) $(call cmd-strip, $(PRIVATE_DST))

# Record installation location for the module.
$(call module-set-installed,$(LOCAL_MODULE),$(LOCAL_INSTALLED_MODULE))
