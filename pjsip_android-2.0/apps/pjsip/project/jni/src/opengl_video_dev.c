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
#include <pjmedia/converter.h>
#include <pjmedia-videodev/videodev_imp.h>
#include <pj/assert.h>
#include <pj/log.h>
#include <pj/os.h>

#include <GLES/gl.h>
#include <GLES/glext.h>

static GLint textureWidth;
static GLint textureHeight;
static void* imageData;
static pj_mutex_t* ogl_mutex;


#define THIS_FILE		"android_video_dev.c"
#define DEFAULT_CLOCK_RATE	90000
#define DEFAULT_WIDTH		640
#define DEFAULT_HEIGHT		480
#define DEFAULT_FPS		25


typedef struct ogl_fmt_info
{
    pjmedia_format_id   fmt_id;
    GLenum format;
    GLenum type;
} ogl_fmt_info;

static ogl_fmt_info ogl_fmts[] =
{

    {PJMEDIA_FORMAT_RGBA,  GL_RGBA, GL_UNSIGNED_BYTE} ,
    //{PJMEDIA_FORMAT_RGB24, GL_RGB, GL_UNSIGNED_SHORT_4_4_4} ,
    /*
    {PJMEDIA_FORMAT_RGB24, 0, 0xFF, 0xFF00, 0xFF0000, 0} ,
    {PJMEDIA_FORMAT_BGRA,  0, 0xFF0000, 0xFF00, 0xFF, 0xFF000000} ,

    {PJMEDIA_FORMAT_DIB  , 0, 0xFF0000, 0xFF00, 0xFF, 0} ,

    {PJMEDIA_FORMAT_YUY2, SDL_YUY2_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_UYVY, SDL_UYVY_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_YVYU, SDL_YVYU_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_I420, SDL_IYUV_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_YV12, SDL_YV12_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_I420JPEG, SDL_IYUV_OVERLAY, 0, 0, 0, 0} ,
    {PJMEDIA_FORMAT_I422JPEG, SDL_YV12_OVERLAY, 0, 0, 0, 0} ,
    */
};


/* ogl_ device info */
struct ogl_dev_info
{
    pjmedia_vid_dev_info	 info;
};

/* ogl_ factory */
struct ogl_factory
{
    pjmedia_vid_dev_factory	 base;
    pj_pool_t			*pool;
    pj_pool_factory		*pf;

    unsigned			 dev_count;
    struct ogl_dev_info	        *dev_info;
};

/* Video stream. */
struct ogl_stream
{
    pjmedia_vid_dev_stream	 base;		    /**< Base stream	    */
    pjmedia_vid_param		 param;		    /**< Settings	    */
    pj_pool_t			*pool;              /**< Memory pool.       */

    pjmedia_vid_cb		 vid_cb;            /**< Stream callback.   */
    void			*user_data;         /**< Application data.  */

    pj_bool_t			 is_quitting;
    pj_bool_t			 is_running;
    pj_bool_t			 render_exited;
    pj_status_t			 status;


    /* For frame conversion */
    pjmedia_converter		*conv;
    pjmedia_conversion_param	 conv_param;
    pjmedia_frame		 conv_buf;

    pjmedia_video_apply_fmt_param vafp;
};


/* Prototypes */
static pj_status_t ogl_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t ogl_factory_destroy(pjmedia_vid_dev_factory *f);
static unsigned    ogl_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t ogl_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					    unsigned index,
					    pjmedia_vid_dev_info *info);
static pj_status_t ogl_factory_default_param(pj_pool_t *pool,
                                             pjmedia_vid_dev_factory *f,
					     unsigned index,
					     pjmedia_vid_param *param);
static pj_status_t ogl_factory_create_stream(
					pjmedia_vid_dev_factory *f,
					const pjmedia_vid_param *param,
					const pjmedia_vid_cb *cb,
					void *user_data,
					pjmedia_vid_dev_stream **p_vid_strm);

static pj_status_t ogl_stream_get_param(pjmedia_vid_dev_stream *strm,
					pjmedia_vid_param *param);
static pj_status_t ogl_stream_get_cap(pjmedia_vid_dev_stream *strm,
				      pjmedia_vid_dev_cap cap,
				      void *value);
static pj_status_t ogl_stream_set_cap(pjmedia_vid_dev_stream *strm,
				      pjmedia_vid_dev_cap cap,
				      const void *value);
static pj_status_t ogl_stream_put_frame(pjmedia_vid_dev_stream *strm,
                                        const pjmedia_frame *frame);
static pj_status_t ogl_stream_start(pjmedia_vid_dev_stream *strm);
static pj_status_t ogl_stream_stop(pjmedia_vid_dev_stream *strm);
static pj_status_t ogl_stream_destroy(pjmedia_vid_dev_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op =
{
    &ogl_factory_init,
    &ogl_factory_destroy,
    &ogl_factory_get_dev_count,
    &ogl_factory_get_dev_info,
    &ogl_factory_default_param,
    &ogl_factory_create_stream
};

static pjmedia_vid_dev_stream_op stream_op =
{
    &ogl_stream_get_param,
    &ogl_stream_get_cap,
    &ogl_stream_set_cap,
    &ogl_stream_start,
    NULL,
    &ogl_stream_put_frame,
    &ogl_stream_stop,
    &ogl_stream_destroy
};


/****************************************************************************
 * Factory operations
 */
/*
 * Init ogl_ video driver.
 */
pjmedia_vid_dev_factory* pjmedia_ogl_factory(pj_pool_factory *pf)
{
    struct ogl_factory *f;
    pj_pool_t *pool;

    pool = pj_pool_create(pf, "ogl video", 1000, 1000, NULL);
    f = PJ_POOL_ZALLOC_T(pool, struct ogl_factory);
    f->pf = pf;
    f->pool = pool;
    f->base.op = &factory_op;

    return &f->base;
}


/* API: init factory */
static pj_status_t ogl_factory_init(pjmedia_vid_dev_factory *f)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;
    struct ogl_dev_info *ddi;
    unsigned i;

    sf->dev_count = 1;
    sf->dev_info = (struct ogl_dev_info*)
 		   pj_pool_calloc(sf->pool, sf->dev_count,
 				  sizeof(struct ogl_dev_info));

    ddi = &sf->dev_info[0];
    pj_bzero(ddi, sizeof(*ddi));
    pj_ansi_strcpy(ddi->info.name, "OpenGL renderer");
    pj_ansi_strcpy(ddi->info.driver, "OpenGL");
    ddi->info.dir = PJMEDIA_DIR_RENDER;
    ddi->info.has_callback = PJ_FALSE;
    ddi->info.caps = PJMEDIA_VID_DEV_CAP_FORMAT |
                     PJMEDIA_VID_DEV_CAP_OUTPUT_RESIZE;

    ddi->info.fmt_cnt = PJ_ARRAY_SIZE(ogl_fmts);
    ddi->info.fmt = (pjmedia_format*)
 		    pj_pool_calloc(sf->pool, ddi->info.fmt_cnt,
 				   sizeof(pjmedia_format));
    for (i = 0; i < ddi->info.fmt_cnt; i++) {
        pjmedia_format *fmt = &ddi->info.fmt[i];
        pjmedia_format_init_video(fmt, ogl_fmts[i].fmt_id,
				  DEFAULT_WIDTH, DEFAULT_HEIGHT,
				  DEFAULT_FPS, 1);
    }

    PJ_LOG(4, (THIS_FILE, "OpenGL initialized"));

    return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t ogl_factory_destroy(pjmedia_vid_dev_factory *f)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;
    pj_pool_t *pool = sf->pool;

    sf->pool = NULL;
    pj_pool_release(pool);

    return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned ogl_factory_get_dev_count(pjmedia_vid_dev_factory *f)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;
    return sf->dev_count;
}

/* API: get device info */
static pj_status_t ogl_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					    unsigned index,
					    pjmedia_vid_dev_info *info)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;

    PJ_ASSERT_RETURN(index < sf->dev_count, PJMEDIA_EVID_INVDEV);

    pj_memcpy(info, &sf->dev_info[index].info, sizeof(*info));

    return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t ogl_factory_default_param(pj_pool_t *pool,
                                             pjmedia_vid_dev_factory *f,
					     unsigned index,
					     pjmedia_vid_param *param)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;
    struct ogl_dev_info *di = &sf->dev_info[index];

    PJ_ASSERT_RETURN(index < sf->dev_count, PJMEDIA_EVID_INVDEV);

    PJ_UNUSED_ARG(pool);

    pj_bzero(param, sizeof(*param));
    if (di->info.dir == PJMEDIA_DIR_CAPTURE_RENDER) {
	param->dir = PJMEDIA_DIR_CAPTURE_RENDER;
	param->cap_id = index;
	param->rend_id = index;
    } else if (di->info.dir & PJMEDIA_DIR_CAPTURE) {
	param->dir = PJMEDIA_DIR_CAPTURE;
	param->cap_id = index;
	param->rend_id = PJMEDIA_VID_INVALID_DEV;
    } else if (di->info.dir & PJMEDIA_DIR_RENDER) {
	param->dir = PJMEDIA_DIR_RENDER;
	param->rend_id = index;
	param->cap_id = PJMEDIA_VID_INVALID_DEV;
    } else {
	return PJMEDIA_EVID_INVDEV;
    }

    /* Set the device capabilities here */
    param->flags = PJMEDIA_VID_DEV_CAP_FORMAT;
    param->fmt.type = PJMEDIA_TYPE_VIDEO;
    param->clock_rate = DEFAULT_CLOCK_RATE;
    pjmedia_format_init_video(&param->fmt, ogl_fmts[0].fmt_id,
			      DEFAULT_WIDTH, DEFAULT_HEIGHT,
			      DEFAULT_FPS, 1);

    return PJ_SUCCESS;
}

static ogl_fmt_info* get_ogl_format_info(pjmedia_format_id id)
{
    unsigned i;

    for (i = 0; i < sizeof(ogl_fmts)/sizeof(ogl_fmts[0]); i++) {
        if (ogl_fmts[i].fmt_id == id)
            return &ogl_fmts[i];
    }

    return NULL;
}



/* API: create stream */
static pj_status_t ogl_factory_create_stream(
					pjmedia_vid_dev_factory *f,
					const pjmedia_vid_param *param,
					const pjmedia_vid_cb *cb,
					void *user_data,
					pjmedia_vid_dev_stream **p_vid_strm)
{
    struct ogl_factory *sf = (struct ogl_factory*)f;
    pj_pool_t *pool;
    struct ogl_stream *strm;
    pj_status_t status;
    pjmedia_video_format_detail *vfd;
    const pjmedia_video_format_info *vfi;


    /* Create and Initialize stream descriptor */
    pool = pj_pool_create(sf->pf, "opengl-dev", 1000, 1000, NULL);
    PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);

    strm = PJ_POOL_ZALLOC_T(pool, struct ogl_stream);
    pj_memcpy(&strm->param, param, sizeof(*param));
    strm->pool = pool;
    pj_memcpy(&strm->vid_cb, cb, sizeof(*cb));
    strm->user_data = user_data;


    /* Create render stream here */
    if (param->dir & PJMEDIA_DIR_RENDER) {
        strm->status = PJ_SUCCESS;
        /*
        status = pj_thread_create(pool, "ogl_thread", create_ogl_thread,
                                  strm, 0, 0, &strm->ogl_thread);
        if (status != PJ_SUCCESS) {
            goto on_error;
        }
        while (strm->status == PJ_SUCCESS && !strm->surf && !strm->overlay)
            pj_thread_sleep(10);
        if ((status = strm->status) != PJ_SUCCESS) {
            goto on_error;
        }
		*/
        pjmedia_format_copy(&strm->conv_param.src, &param->fmt);
        pjmedia_format_copy(&strm->conv_param.dst, &param->fmt);

        status = pjmedia_converter_create(NULL, pool, &strm->conv_param,
                                          &strm->conv);
    }

    /* Apply the remaining settings */
    if (param->flags & PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW) {
		ogl_stream_set_cap(&strm->base,
						PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW,
								&param->window);
    }


    vfi = pjmedia_get_video_format_info(pjmedia_video_format_mgr_instance(),
                                        strm->param.fmt.id);

    vfd = pjmedia_format_get_video_format_detail(&strm->param.fmt, PJ_TRUE);

    textureWidth = (GLint)vfd->size.w;
    textureHeight = (GLint)vfd->size.h;
    PJ_LOG(4, (THIS_FILE, "We expect : %d x %d x %d", textureWidth , textureHeight, vfi->bpp/8));
    pj_size_t dataSize = textureWidth * textureHeight * vfi->bpp/8;
    imageData = pj_pool_alloc(sf->pool, dataSize);
    pj_bzero(imageData, dataSize);
    pj_mutex_create_simple(strm->pool, "opengl-es", &ogl_mutex);


    /* Done */
    strm->base.op = &stream_op;
    *p_vid_strm = &strm->base;

    return PJ_SUCCESS;

on_error:
    ogl_stream_destroy(&strm->base);
    return status;
}

/* API: Get stream info. */
static pj_status_t ogl_stream_get_param(pjmedia_vid_dev_stream *s,
					pjmedia_vid_param *pi)
{
    struct ogl_stream *strm = (struct ogl_stream*)s;

    PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

    pj_memcpy(pi, &strm->param, sizeof(*pi));

    if (ogl_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW, NULL) == PJ_SUCCESS) {
        pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
    }

    return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t ogl_stream_get_cap(pjmedia_vid_dev_stream *s,
				      pjmedia_vid_dev_cap cap,
				      void *pval)
{
    struct ogl_stream *strm = (struct ogl_stream*)s;

    PJ_UNUSED_ARG(strm);

    PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

    if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW) {
    	return PJ_SUCCESS;
    } else if (cap == PJMEDIA_VID_DEV_CAP_FORMAT) {
        return PJ_SUCCESS;
    } else {
    	return PJMEDIA_EVID_INVCAP;
    }
}

/* API: set capability */
static pj_status_t ogl_stream_set_cap(pjmedia_vid_dev_stream *s,
				      pjmedia_vid_dev_cap cap,
				      const void *pval)
{
    struct ogl_stream *strm = (struct ogl_stream*)s;

    PJ_UNUSED_ARG(strm);

    PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

    if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW) {
    	return PJ_SUCCESS;
    } else if (cap == PJMEDIA_VID_DEV_CAP_FORMAT) {
    	return PJ_SUCCESS;
    }

    return PJMEDIA_EVID_INVCAP;
}

/* API: Put frame from stream */
static pj_status_t ogl_stream_put_frame(pjmedia_vid_dev_stream *strm,
                                        const pjmedia_frame *frame)
{
    struct ogl_stream *stream = (struct ogl_stream*)strm;
    int i;
    pj_status_t status = PJ_SUCCESS;


    PJ_LOG(3, (THIS_FILE, "Put a frame ...."));
    if (!stream->is_running) {
        stream->render_exited = PJ_TRUE;
        goto on_return;
    }

    if (frame->size==0 || frame->buf==NULL){
    	goto on_return;
    }

    pj_mutex_lock(ogl_mutex);
    pj_memcpy(imageData, frame->buf, frame->size);
    pj_mutex_unlock(ogl_mutex);


on_return:
    return status;
}

/* API: Start stream. */
static pj_status_t ogl_stream_start(pjmedia_vid_dev_stream *strm)
{
    struct ogl_stream *stream = (struct ogl_stream*)strm;

    PJ_LOG(4, (THIS_FILE, "Starting opengl video stream"));

    stream->is_running = PJ_TRUE;
    stream->render_exited = PJ_FALSE;

    return PJ_SUCCESS;
}

/* API: Stop stream. */
static pj_status_t ogl_stream_stop(pjmedia_vid_dev_stream *strm)
{
    struct ogl_stream *stream = (struct ogl_stream*)strm;
    unsigned i;

    PJ_LOG(4, (THIS_FILE, "Stopping opengl video stream"));

    /* Wait for renderer put_frame() to finish */
    stream->is_running = PJ_FALSE;
    for (i=0; !stream->render_exited && i<100; ++i){
    	pj_thread_sleep(10);
    }

    pj_mutex_lock(ogl_mutex);

    textureHeight = 0;
    textureWidth = 0;

    pj_mutex_unlock(ogl_mutex);
    return PJ_SUCCESS;
}


/* API: Destroy stream. */
static pj_status_t ogl_stream_destroy(pjmedia_vid_dev_stream *strm)
{
    struct ogl_stream *stream = (struct ogl_stream*)strm;

    PJ_ASSERT_RETURN(stream != NULL, PJ_EINVAL);

    ogl_stream_stop(strm);

    pj_pool_release(stream->pool);

    return PJ_SUCCESS;
}



PJ_DECL(pjmedia_vid_dev_factory*) pjmedia_ogl_surface_init(int width,
		int height) {
//

}


PJ_DECL(pjmedia_vid_dev_factory*) pjmedia_ogl_surface_draw(){
	//
	int i = 0;

	if(textureWidth > 0 && textureHeight > 0){

		pj_mutex_lock(ogl_mutex);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid *)imageData);
		pj_mutex_unlock(ogl_mutex);



	}

}
