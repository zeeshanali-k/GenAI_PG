//
//  MediaPipeModelManager.swift
//  iosApp
//
//  Created by Zeeshan Ali on 25/01/2026.
//

import Foundation
import composeApp
import MediaPipeTasksGenAI


class MediaPipeModelManager: SwiftModelManager {


    var llmInference: LlmInference?


    func generateResponseAsync(inputText: String, progress: @escaping (String) -> Void, completion: @escaping (String, String?) -> Void) async throws {
        try llmInference?.generateResponseAsync(inputText: inputText) { partialResponse, error in
            // progress
            if let e = error {
                print(" \(e)")
                completion("", e.localizedDescription)
                return
            }
            if let partial = partialResponse {
                progress(partial)
            }
        } completion: {
            completion("", nil)
        }
    }

    func loadModel(model: Model) {

        print("model id-> \(model.id.deletingSuffix(".bin"))")
        guard let path = Bundle.main.path(forResource: model.id.deletingSuffix(".bin"),
                                          ofType: "bin")
        else {
            return
        }
        print("path-> \(path)")
        let llmOptions = LlmInference.Options(modelPath: path)
        llmOptions.maxTokens = Int(model.maxTokens)
        // llmOptions.maxTopk = Int(model.topK)
//        llmOptions.temperature = 0.9
        do {
            print("Loading")
            llmInference = try LlmInference(options: llmOptions)
            print("Loaded")
        } catch {
            print("Failed to load")
            // You may choose to log or handle the error here
            llmInference = nil
        }
    }


    func sizeInTokens(text: String) -> Int32 {
        return -1
    }


}
