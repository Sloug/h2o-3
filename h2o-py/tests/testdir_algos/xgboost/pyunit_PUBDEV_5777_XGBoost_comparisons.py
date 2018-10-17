import xgboost as xgb
import time


from h2o.estimators.xgboost import *
from tests import pyunit_utils

'''
PUBDEV-5777: enable H2OXGBoost and native XGBoost comparison.

To ensure that H2OXGBoost and native XGBoost performance provide the same result, we propose to do the following:
1. run H2OXGBoost with H2OFrame and parameter setting, save model and result
2. Call Python API to convert H2OFrame to XGBoost frame and H2OXGBoost parameter to XGBoost parameters.
3. Run native XGBoost with data frame and parameters from 2.  Should get the same result as in 1.

Parameters in native XGBoost:
booster defaul to gbtree
silent default to 0
nthread default to maximum number of threads available if not specified
disable_default_eval_metric default to 0
num_pbuffer automatically set
num_feature automatically set
eta/learning_rate default to 0.3
max_depth default to 6
min_child_weight default to 1
max_delta_step default to 0
subsample default to 1
colsample_bytree default to 1
colsample_by_level default to 1
lambda/reg_lambda default to 1
alpha/reg_alpha default to 0
tree_method default to auto
sketch_eps default to 0.03
scale_pos_weight default to 1
updater default to grow_colmaker, prune
refresh_leaf default to 1
process_type default to default
grow_policy default depthwise
max_leaves default to 0
max_bin default to 256
predictor default to cpu_predictor

Addition ones for DART booster
smaple_type default to uniform
normalize_type default to tree
rate_drop default to 0.0
one_drop default to 0.0
skip_drop default to 0.0

For Linear Booster
lambda/reg_lambda default to 0
alpha/reg_alpha default to 0
updater default to shotgun
feature_selector default to cyclic
top_k default to 0

Parameters for Tweedie Regression objective=reg:tweedie
tweedie_variance_power default to 1.5

learning Task parameters:
objective default to reg:linear
base_score default to 0.5
eval_metric default according to objective
seed default to 0
'''
def comparison_test():
    assert H2OXGBoostEstimator.available() is True
    ret = h2o.cluster()
    if len(ret.nodes) == 1:
        runSeed = 1
        dataSeed = 17
        testTol = 1e-6
        ntrees = 17
        maxdepth = 5
        nrows = 10000
        ncols = 10
        factorL = 20
        numCols = 5
        enumCols = ncols-numCols
        responseL = 4
        # CPU Backend is forced for the results to be comparable
        h2oParamsD = {"ntrees":ntrees, "max_depth":maxdepth, "seed":runSeed, "learn_rate":0.7, "col_sample_rate_per_tree" : 0.9,
                     "min_rows" : 5, "score_tree_interval": ntrees+1, "dmatrix_type":"dense","tree_method": "exact", "backend":"cpu"}
        nativeParam = {'colsample_bytree': h2oParamsD["col_sample_rate_per_tree"],
                       'tree_method': 'exact',
                       'seed': h2oParamsD["seed"],
                       'booster': 'gbtree',
                       'objective': 'multi:softprob',
                       'lambda': 0.0,
                       'eta': h2oParamsD["learn_rate"],
                       'grow_policy': 'depthwise',
                       'alpha': 0.0,
                       'subsample': 1.0,
                       'colsample_bylevel': 1.0,
                       'max_delta_step': 0.0,
                       'min_child_weight': h2oParamsD["min_rows"],
                       'gamma': 0.0,
                       'max_depth': h2oParamsD["max_depth"],
                       'num_class':responseL}
        trainFile = pyunit_utils.genTrainFrame(nrows, numCols, enumCols=enumCols, enumFactors=factorL,
                                               responseLevel=responseL, miscfrac=0.01,randseed=dataSeed)
        myX = trainFile.names
        y='response'
        myX.remove(y)
        enumCols = myX[0:enumCols]

        h2oModelD = H2OXGBoostEstimator(**h2oParamsD)
        # gather, print and save performance numbers for h2o model
        h2oModelD.train(x=myX, y=y, training_frame=trainFile)
        h2oTrainTimeD = h2oModelD._model_json["output"]["run_time"]
        time1 = time.time()
        h2oPredictD = h2oModelD.predict(trainFile)
        h2oPredictTimeD = time.time()-time1

        # train the native XGBoost
        nativeTrain = pyunit_utils.convertH2OFrameToDMatrix(trainFile, y, enumCols=enumCols)
        nrounds = ntrees
        nativeModel = xgb.train(params=nativeParam,
                                dtrain=nativeTrain, num_boost_round=nrounds)
        modelInfo = nativeModel.get_dump()
        print(modelInfo)
        print("num_boost_round: {1}, Number of trees built: {0}".format(len(modelInfo), nrounds))
        nativeTrainTime = time.time()-time1
        time1=time.time()
        nativePred = nativeModel.predict(data=nativeTrain, ntree_limit=ntrees)
        nativeScoreTime = time.time()-time1

        pyunit_utils.summarizeResult_multinomial(h2oPredictD, nativePred, h2oTrainTimeD, nativeTrainTime, h2oPredictTimeD,
                                              nativeScoreTime, tolerance=testTol)
    else:
        print("********  Test skipped.  This test cannot be performed in multinode environment.")

if __name__ == "__main__":
    pyunit_utils.standalone_test(comparison_test)
else:
    comparison_test()
