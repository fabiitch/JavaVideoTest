#pragma once
#include <gst/gst.h>
#include <gst/gl/gl.h>

#ifdef _WIN32
  #define NZ_API __declspec(dllexport)
#else
  #define NZ_API
#endif

// Crée un GstContext("gst.gl.app_context") avec "context" = glctx
NZ_API GstContext* nz_gst_make_gl_app_context(GstGLContext* glctx);

// Applique le GstContext à un element (gst_element_set_context)
NZ_API void nz_gst_element_set_context(GstElement* elem, GstContext* ctx);
