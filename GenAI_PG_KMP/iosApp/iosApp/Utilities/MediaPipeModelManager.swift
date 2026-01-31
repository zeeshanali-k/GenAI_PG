//
//  MediaPipeModelManager.swift
//  iosApp
//
//  Created by Zeeshan Ali on 25/01/2026.
//

import Foundation
import composeApp
import MediaPipeTasksGenAI
import MediaPipeTasksGenAIC


class MediaPipeModelManager: SwiftModelManager {


    var llmInference: LlmInference?
    var llmInferenceSession: LlmInference.Session?

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

    func loadModel(model: Model) -> Bool {

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
            createSession(model)
            print("Loaded")
            return true
        } catch {
            print("Failed to load")
            // You may choose to log or handle the error here
            llmInference = nil
            return false
        }
    }

    func createSession(_ model: Model) {
        // Ensure we have a valid inference object
        guard let llmInference else {
            print("createSession: llmInference is nil; did you call loadModel() successfully?")
            llmInferenceSession = nil
            return
        }

        let options = LlmInference.Session.Options()
        options.topk = Int(model.topK)
        options.randomSeed = Int(model.randomSeed)
        options.temperature = model.temperature
        options.topp = model.topP

        do {
            llmInferenceSession = try LlmInference.Session(llmInference: llmInference, options: options)
        } catch {
            print("Failed to create LlmInference.Session: \(error)")
            llmInferenceSession = nil
        }
    }

    func close() {
        llmInferenceSession = nil
        llmInference = nil
    }

    func stopResponseGeneration() {

    }


    func sizeInTokens(text: String) -> Int32 {
        return -1
    }


}
