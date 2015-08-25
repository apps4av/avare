#Copyright Apps4Av Inc.
#Author Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

MAKEFLAGS = --quiet

# define CYCLE
ifndef CYCLE
$(error !! CYCLE must be defined before make can proceed, use export CYCLE=XXXX)
endif


export BASE_DIR=$(CURDIR)

export BUILD_DIR=$(BASE_DIR)/final/${CYCLE}
export TOOLS_DIR=$(BASE_DIR)/tools

# Include host OS stuff
-include tools/host.mk

# This file includes the dirs that will be built
-include tools/modules.mk

# All modules to be built
.PHONY: all
all:
	@if [ ! -d $(BUILD_DIR) ]; then $(MKDIR) $(BUILD_DIR); fi
	@for m in $(MODULES); \
	do \
		if (!(	$(CHDIR) $(BASE_DIR)/$$m && \
			MODULE=$$m MODULE_DIR=$(BASE_DIR)/$$m \
			$(MAKE) -f config.mk output )) \
		then \
			exit -1; \
		fi; \
	done

# Clean all modules
.PHONY: clean
clean:
	@if [ -d $(BUILD_DIR) ]; then \
	for m in $(MODULES); \
    do \
        if (!(  $(CHDIR) $(BASE_DIR)/$$m && \
            MODULE=$$m MODULE_DIR=$(BASE_DIR)/$$m \
            $(MAKE) -f config.mk clean )) \
        then \
            exit -1; \
        fi; \
    done \
	fi


# final
.PHONY: prep 
prep:
	${CHDIR} ${BUILD_DIR} && \
	for file in *.zip; \
	do \
	    ${TOOLS_DIR}/zip.pl $$file ${CYCLE}; \
	done
	chmod 644 ${BUILD_DIR}/*.zip
