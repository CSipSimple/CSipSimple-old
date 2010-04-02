/**
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pjsip.pjsua;

public class pj_pool_t {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pj_pool_t(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pj_pool_t obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pj_pool_t(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setObj_name(String value) {
    pjsuaJNI.pj_pool_t_obj_name_set(swigCPtr, this, value);
  }

  public String getObj_name() {
    return pjsuaJNI.pj_pool_t_obj_name_get(swigCPtr, this);
  }

  public void setFactory(SWIGTYPE_p_pj_pool_factory value) {
    pjsuaJNI.pj_pool_t_factory_set(swigCPtr, this, SWIGTYPE_p_pj_pool_factory.getCPtr(value));
  }

  public SWIGTYPE_p_pj_pool_factory getFactory() {
    long cPtr = pjsuaJNI.pj_pool_t_factory_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pj_pool_factory(cPtr, false);
  }

  public void setFactory_data(byte[] value) {
    pjsuaJNI.pj_pool_t_factory_data_set(swigCPtr, this, value);
  }

  public byte[] getFactory_data() {
	return pjsuaJNI.pj_pool_t_factory_data_get(swigCPtr, this);
}

  public void setCapacity(long value) {
    pjsuaJNI.pj_pool_t_capacity_set(swigCPtr, this, value);
  }

  public long getCapacity() {
    return pjsuaJNI.pj_pool_t_capacity_get(swigCPtr, this);
  }

  public void setIncrement_size(long value) {
    pjsuaJNI.pj_pool_t_increment_size_set(swigCPtr, this, value);
  }

  public long getIncrement_size() {
    return pjsuaJNI.pj_pool_t_increment_size_get(swigCPtr, this);
  }

  public void setBlock_list(SWIGTYPE_p_pj_pool_block value) {
    pjsuaJNI.pj_pool_t_block_list_set(swigCPtr, this, SWIGTYPE_p_pj_pool_block.getCPtr(value));
  }

  public SWIGTYPE_p_pj_pool_block getBlock_list() {
    return new SWIGTYPE_p_pj_pool_block(pjsuaJNI.pj_pool_t_block_list_get(swigCPtr, this), true);
  }

  public void setCallback(SWIGTYPE_p_pj_pool_callback value) {
    pjsuaJNI.pj_pool_t_callback_set(swigCPtr, this, SWIGTYPE_p_pj_pool_callback.getCPtr(value));
  }

  public SWIGTYPE_p_pj_pool_callback getCallback() {
    long cPtr = pjsuaJNI.pj_pool_t_callback_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pj_pool_callback(cPtr, false);
  }

  public pj_pool_t() {
    this(pjsuaJNI.new_pj_pool_t(), true);
  }

}
