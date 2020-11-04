package com.aws.proserve.korea.event.generator.view;

public enum AwsEventGeneratorTaskInfoState {
	INIT,
	ERROR_DURING_CREATION,
	CREATED,
	ATTACHED,
	ERROR_DURING_ATTACH,
	DETACHED,
	ERROR_DURING_UPDATE,
	NOT_RUNNING,
	UNKNOWN_ERROR,
}
