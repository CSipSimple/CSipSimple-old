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

# this file is included repeatedly from build/core/main.mk
# and is used to prepare for app-specific build rules.
#

$(call assert-defined,_app)

_map := NDK_APP.$(_app)

# ok, let's parse all Android.mk source files in order to build
# the modules for this app.
#

# Restore the APP_XXX variables just for this pass as NDK_APP_XXX
#
NDK_APP_NAME           := $(_app)
NDK_APP_APPLICATION_MK := $(call get,$(_map),Application.mk)

$(foreach __name,$(NDK_APP_VARS),\
  $(eval NDK_$(__name) := $(call get,$(_map),$(__name)))\
)

# which platform/abi/toolchain are we going to use?
TARGET_PLATFORM := $(call get,$(_map),APP_PLATFORM)

# set release/debug build flags. We always use the -g flag because
# we generate symbol versions of the binaries that are later stripped
# when they are copied to the final project's libs/<abi> directory.
#
ifeq ($(NDK_APP_OPTIM),debug)
  NDK_APP_CFLAGS := -O0 -g $(NDK_APP_CFLAGS) -DANDROID_APP_PLATFORM=$(TARGET_PLATFORM)
else
  NDK_APP_CFLAGS := -O2 -DNDEBUG -g $(NDK_APP_CFLAGS) -DANDROID_APP_PLATFORM=$(TARGET_PLATFORM)
endif

# make the application depend on the modules it requires
.PHONY: ndk-app-$(_app)
ndk-app-$(_app): $(NDK_APP_MODULES)
all: ndk-app-$(_app)


# The ABI(s) to use
APP_ABI := $(strip $(NDK_APP_ABI))
ifndef APP_ABI
    # the default ABI for now is armeabi
    APP_ABI := armeabi
endif

# check the target ABIs for this application
_bad_abis = $(strip $(filter-out $(NDK_ALL_ABIS),$(APP_ABI)))
ifneq ($(_bad_abis),)
    $(call __ndk_info,NDK Application '$(_app)' targets unknown ABI(s): $(_bad_abis))
    $(call __ndk_info,Please fix the APP_ABI definition in $(NDK_APP_APPLICATION_MK))
    $(call __ndk_error,Aborting)
endif

# Extract the debuggable flag from the application's manifest
# NOTE: To make unit-testing simpler, handle the case where there is no manifest.
#
NDK_APP_DEBUGGABLE := false
NDK_APP_MANIFEST := $(strip $(wildcard $(NDK_APP_PROJECT_PATH)/AndroidManifest.xml))
ifdef NDK_APP_MANIFEST
    NDK_APP_DEBUGGABLE := $(shell $(HOST_AWK) -f $(BUILD_AWK)/extract-debuggable.awk $(NDK_APP_MANIFEST))
endif

ifdef NDK_LOG
  ifeq ($(NDK_APP_DEBUGGABLE),true)
    $(call ndk_log,Application '$(_app)' *is* debuggable)
  else
    $(call ndk_log,Application '$(_app)' is not debuggable)
  endif
endif

# Clear all installed binaries for this application
# This ensures that if the build fails, you're not going to mistakenly
# package an obsolete version of it. Or if you change the ABIs you're targetting,
# you're not going to leave a stale shared library for the old one.
#
ifeq ($(NDK_APP.$(_app).cleaned_binaries),)
    NDK_APP.$(_app).cleaned_binaries := true
    clean-installed-binaries::
	$(hide) rm -f $(NDK_ALL_ABIS:%=$(NDK_APP_PROJECT_PATH)/libs/%/lib*.so)
	$(hide) rm -f $(NDK_ALL_ABIS:%=$(NDK_APP_PROJECT_PATH)/libs/%/gdbserver)
	$(hide) rm -f $(NDK_ALL_ABIS:%=$(NDK_APP_PROJECT_PATH)/libs/%/gdb.setup)
endif

$(foreach _abi,$(APP_ABI),\
    $(eval TARGET_ARCH_ABI := $(_abi))\
    $(eval include $(BUILD_SYSTEM)/setup-abi.mk) \
)
