/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of pjsip_android.
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

#include <pjmedia-videodev/videodev_imp.h>
#include <pj/assert.h>
#include <pj/log.h>
#include <pj/os.h>
#include <pj/unicode.h>


#define THIS_FILE		"android_video_dev.c"


#define MAX_DEV_CNT     8


typedef struct android_video_dev_factory
{
    pjmedia_vid_dev_factory	 base;
    pj_pool_factory		*pf;
    pj_pool_t                   *pool;
} android_video_dev_factory;


typedef struct android_video_dev_stream
{
    pjmedia_vid_stream           base;
    android_video_dev_factory              *factory;
    pj_pool_t                   *pool;
    pjmedia_vid_param            param;
} android_video_dev_stream;


/* Prototypes */
static pj_status_t android_video_dev_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t android_video_dev_factory_destroy(pjmedia_vid_dev_factory *f);
static unsigned    android_video_dev_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t android_video_dev_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					      unsigned index,
					      pjmedia_vid_dev_info *info);
static pj_status_t android_video_dev_factory_default_param(pj_pool_t *pool,
                                                pjmedia_vid_dev_factory *f,
					        unsigned index,
					        pjmedia_vid_param *param);
static pj_status_t android_video_dev_factory_create_stream(pjmedia_vid_dev_factory *f,
					       const pjmedia_vid_param *param,
					       const pjmedia_vid_cb *cb,
					       void *user_data,
					       pjmedia_vid_stream **p_vid_strm);

static pj_status_t android_video_dev_stream_get_param(pjmedia_vid_stream *strm,
					  pjmedia_vid_param *param);
static pj_status_t android_video_dev_stream_get_cap(pjmedia_vid_stream *strm,
				        pjmedia_vid_dev_cap cap,
				        void *value);
static pj_status_t android_video_dev_stream_set_cap(pjmedia_vid_stream *strm,
				        pjmedia_vid_dev_cap cap,
				        const void *value);
static pj_status_t android_video_dev_stream_start(pjmedia_vid_stream *strm);
static pj_status_t android_video_dev_stream_get_frame(pjmedia_vid_stream *s,
                                           pjmedia_frame *frame);
static pj_status_t android_video_dev_stream_stop(pjmedia_vid_stream *strm);
static pj_status_t android_video_dev_stream_destroy(pjmedia_vid_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op =
{
    &android_video_dev_factory_init,
    &android_video_dev_factory_destroy,
    &android_video_dev_factory_get_dev_count,
    &android_video_dev_factory_get_dev_info,
    &android_video_dev_factory_default_param,
    &android_video_dev_factory_create_stream
};

static pjmedia_vid_stream_op stream_op =
{
    &android_video_dev_stream_get_param,
    &android_video_dev_stream_get_cap,
    &android_video_dev_stream_set_cap,
    &android_video_dev_stream_start,
    &android_video_dev_stream_get_frame,
    NULL,
    &android_video_dev_stream_stop,
    &android_video_dev_stream_destroy
};



/****************************************************************************
 * Factory operations
 */
/*
 * Init android_video_dev_ video driver.
 */
pjmedia_vid_dev_factory* pjmedia_android_video_dev_factory(pj_pool_factory *pf)
{
    android_video_dev_factory *f;
    pj_pool_t *pool;

    pool = pj_pool_create(pf, "android_video_dev_cap_dev", 1000, 1000, NULL);
    f = PJ_POOL_ZALLOC_T(pool, android_video_dev_factory);

    f->pool = pool;
    f->pf = pf;
    f->base.op = &factory_op;
    PJ_LOG(4, (THIS_FILE, "Factory vid android"));

    return &f->base;
}


/* API: init factory */
static pj_status_t android_video_dev_factory_init(pjmedia_vid_dev_factory *f)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;


    PJ_LOG(4, (THIS_FILE, "Init vid android"));

    return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t android_video_dev_factory_destroy(pjmedia_vid_dev_factory *f)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;


    return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned android_video_dev_factory_get_dev_count(pjmedia_vid_dev_factory *f)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;
    PJ_LOG(4, (THIS_FILE, "get android dev nbrs"));
    return 1;
}

/* API: get device info */
static pj_status_t android_video_dev_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					      unsigned index,
					      pjmedia_vid_dev_info *info)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;

    PJ_LOG(4, (THIS_FILE, "get dev infos"));


    return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t android_video_dev_factory_default_param(pj_pool_t *pool,
                                                pjmedia_vid_dev_factory *f,
					        unsigned index,
					        pjmedia_vid_param *param)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;
    pjmedia_video_format_detail *fmt_detail;

    PJ_LOG(4, (THIS_FILE, "Get default params"));
    pj_bzero(param, sizeof(*param));
    param->dir = PJMEDIA_DIR_CAPTURE;
    param->cap_id = PJMEDIA_VID_INVALID_DEV;
    param->rend_id = PJMEDIA_VID_INVALID_DEV;

    /* Set the device capabilities here */
    param->flags = PJMEDIA_VID_DEV_CAP_FORMAT;
   // pj_memcpy(&param->fmt, &info->base.fmt[0], sizeof(param->fmt));
    param->clock_rate = 90000;
    pjmedia_format_init_video(&param->fmt, 1, 320, 240, 25, 1);


    return PJ_SUCCESS;
}



/* API: create stream */
static pj_status_t android_video_dev_factory_create_stream(pjmedia_vid_dev_factory *f,
					       const pjmedia_vid_param *param,
					       const pjmedia_vid_cb *cb,
					       void *user_data,
					       pjmedia_vid_stream **p_vid_strm)
{
    android_video_dev_factory *ff = (android_video_dev_factory*)f;
    pj_pool_t *pool;
    android_video_dev_stream *strm;

    PJ_ASSERT_RETURN(f && param && p_vid_strm, PJ_EINVAL);
    PJ_ASSERT_RETURN(param->dir == PJMEDIA_DIR_CAPTURE, PJ_EINVAL);
    PJ_ASSERT_RETURN((unsigned)param->cap_id < ff->dev_count, PJ_EINVAL);
    PJ_ASSERT_RETURN(param->fmt.detail_type == PJMEDIA_FORMAT_DETAIL_VIDEO &&
                     param->fmt.detail, PJ_EINVAL);

    PJ_UNUSED_ARG(cb);
    PJ_UNUSED_ARG(user_data);

    /* Create and Initialize stream descriptor */
    pool = pj_pool_create(ff->pf, "androvideo-dev", 1000, 1000, NULL);
    PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);

    strm = PJ_POOL_ZALLOC_T(pool, struct android_video_dev_stream);
    strm->factory = (android_video_dev_factory*)f;
    strm->pool = pool;
    pj_memcpy(&strm->param, param, sizeof(*param));

    /* Done */
    strm->base.op = &stream_op;
    *p_vid_strm = &strm->base;

    return PJ_SUCCESS;
}

/* API: Get stream info. */
static pj_status_t android_video_dev_stream_get_param(pjmedia_vid_stream *s,
					   pjmedia_vid_param *pi)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;

    PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

    pj_memcpy(pi, &strm->param, sizeof(*pi));

    return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t android_video_dev_stream_get_cap(pjmedia_vid_stream *s,
				        pjmedia_vid_dev_cap cap,
				        void *pval)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;

    PJ_UNUSED_ARG(strm);
    PJ_UNUSED_ARG(cap);
    PJ_UNUSED_ARG(pval);

    return PJMEDIA_EVID_INVCAP;
}

/* API: set capability */
static pj_status_t android_video_dev_stream_set_cap(pjmedia_vid_stream *s,
				        pjmedia_vid_dev_cap cap,
				        const void *pval)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;

    PJ_UNUSED_ARG(strm);
    PJ_UNUSED_ARG(cap);
    PJ_UNUSED_ARG(pval);

    return PJMEDIA_EVID_INVCAP;
}


/* API: Start stream. */
static pj_status_t android_video_dev_stream_start(pjmedia_vid_stream *s)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;
    pj_status_t status;

status = PJ_SUCCESS;
    PJ_LOG(4, (THIS_FILE, "Starting ffmpeg capture stream"));

    return status;
}


/* API: Get frame from stream */
static pj_status_t android_video_dev_stream_get_frame(pjmedia_vid_stream *s,
                                           pjmedia_frame *frame)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;
    int err;

    pj_bzero(frame, sizeof(*frame));
    frame->type = PJMEDIA_FRAME_TYPE_VIDEO;
    frame->buf = NULL;
    frame->size = 0;

    return PJ_SUCCESS;
}


/* API: Stop stream. */
static pj_status_t android_video_dev_stream_stop(pjmedia_vid_stream *s)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;

    PJ_LOG(4, (THIS_FILE, "Stopping android dev capture stream"));


    return PJ_SUCCESS;
}


/* API: Destroy stream. */
static pj_status_t android_video_dev_stream_destroy(pjmedia_vid_stream *s)
{
    android_video_dev_stream *strm = (android_video_dev_stream*)s;

    PJ_ASSERT_RETURN(strm != NULL, PJ_EINVAL);

    android_video_dev_stream_stop(s);

    pj_pool_release(strm->pool);

    return PJ_SUCCESS;
}


